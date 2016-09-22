#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <stdlib.h>

#include <locale.h>
#include <gpgme.h>

#include "gpgmeutils.h"
#include "com_freiheit_gnupg_GnuPGContext.h"

JavaVM* _jvm;


JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved)
{
    _jvm = vm;

    // this is required to setup gpgme properly, otherwise it crashes
    // https://www.gnupg.org/documentation/manuals/gpgme/Multi-Threading.html
    gpgme_check_version("1.5.0");

    // TODO set locale from the JavaVM's config
    setlocale(LC_ALL, "");
    gpgme_set_locale(NULL, LC_CTYPE, setlocale(LC_CTYPE, NULL));
#ifdef LC_MESSAGES
    gpgme_set_locale(NULL, LC_MESSAGES, setlocale(LC_MESSAGES, NULL));
#endif

    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeGetEngineInfo(JNIEnv* env, jobject self)
{
    gpgme_engine_info_t engineInfo;
    gpgme_get_engine_info(&engineInfo);

    jclass cls = (*env)->GetObjectClass(env, self);

    UTILS_setStringMember(env, self, cls, "_version", engineInfo->version);
    UTILS_setStringMember(env, self, cls, "_filename", engineInfo->file_name);
    UTILS_setStringMember(env, self, cls, "_reqversion",
                          engineInfo->req_version);
    UTILS_setIntMember(env, self, cls, "_protocol", engineInfo->protocol);
}

JNIEXPORT jlong JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeNew(JNIEnv* env, jobject self)
{
    gpgme_ctx_t ctx;
    gpgme_new(&ctx);
    gpgme_set_armor(ctx, 1);
    gpgme_set_textmode(ctx, 1);
    gpgme_set_keylist_mode(ctx, GPGME_KEYLIST_MODE_LOCAL);

    return LNG(ctx);
}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeOpEncrypt(JNIEnv* env,
        jobject self,
        jlong context,
        jlongArray recipients,
        jlong plain,
        jlong cipher)
{
    gpgme_error_t err;

    //how many keys from recipients did we receive?
    jsize len = (*env)->GetArrayLength(env, recipients);

    //allocate a new array with one field larger then number of recipient keys
    gpgme_key_t keys[len + 1];  //allowed in C99 Standard...and useful!!!

    // disabled on werners behalf (33% of the work)
    // overriden bei sebastian.mangels@freiheit.com,
    // we need ascii armor.
    //gpgme_set_armor( CONTEXT(context), 0 );

    if (UTILS_copyRecipientsFromJvm(env, recipients, keys) < 1) {
        return;
    }

    err = gpgme_data_rewind(DATA(plain));   //TODO: Use seek instead of rewind
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }

    err = gpgme_data_rewind(DATA(cipher));   //TODO: Use seek instead of rewind
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }
    //call gpgme library function for encryption
    err = gpgme_op_encrypt(CONTEXT(context), keys, GPGME_ENCRYPT_ALWAYS_TRUST,
                           DATA(plain), DATA(cipher));
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }

    /*   err = gpgme_op_encrypt_start(CONTEXT(context), keys, GPGME_ENCRYPT_ALWAYS_TRUST, (gpgme_data_t)plain, (gpgme_data_t)cipher); */
    /*   if(UTILS_onErrorThrowException(env, err)){ */
    /*     return; */
    /*   } */
    /*   gpgme_ctx_t waitedOn = gpgme_wait(CONTEXT(context), err, 1);//HANG UNTIL COMPLETED! */
    /*   if(waitedOn != NULL && UTILS_onErrorThrowException(env, err)){ */
    /*     return; */
    /*   } */

}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeOpDecrypt(JNIEnv* env,
        jobject self,
        jlong context,
        jlong cipher,
        jlong plain)
{
    gpgme_error_t err;

    err = gpgme_data_rewind(DATA(cipher));   //TODO: Use seek instead of rewind
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }

    err = gpgme_data_rewind(DATA(plain));   //TODO: Use seek instead of rewind
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }

    err = gpgme_op_decrypt(CONTEXT(context), DATA(cipher), DATA(plain));
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }
    /*   err = gpgme_op_decrypt_start(CONTEXT(context), (gpgme_data_t)cipher, (gpgme_data_t)plain); */
    /*   if(UTILS_onErrorThrowException(env, err)){ */
    /*     return; */
    /*   } */
    /*   gpgme_ctx_t waitedOn = gpgme_wait(CONTEXT(context), err, 1);//HANG UNTIL COMPLETED! */
    /*   if(waitedOn != NULL && UTILS_onErrorThrowException(env, err)){ */
    /*     return; */
    /*   } */
}

static void flush_data(gpgme_data_t dh)
{
    char buf[100];
    int ret;

    ret = gpgme_data_seek(dh, 0, SEEK_SET);

    while ((ret = gpgme_data_read(dh, buf, 100)) > 0)
        fwrite(buf, ret, 1, stdout);

}


gpgme_error_t
edit_fnc(void* opaque, gpgme_status_code_t status, const char* args, int fd)
{
    char* result = NULL;
    gpgme_data_t out = (gpgme_data_t)opaque;

    fputs("[-- Response --]\n", stdout);
    flush_data(out);

    fprintf(stdout, "[-- Code: %i, %s --]\n", status, args);

    if (fd >= 0) {
        if (!strcmp(args, "keyedit.prompt")) {
            static int switcher = 0;

            if (!switcher) {
                result = "passwd";
                switcher++;
            } else {
                result = "save";
                switcher--;
            }

        } else if (!strcmp(args, "keyedit.save.okay")) {
            result = "Y";
        } else if (!strcmp(args, "keygen.valid")) {
            result = "0";
        } else if (!strcmp(args, "change_passwd.empty.okay")) {
            result = "N";
        }
    }

    if (result) {
        fprintf(stdout, "[-- Command: %s --]\n", result);
        write(fd, result, strlen(result));
        write(fd, "\n", 1);
    }
    return 0;
}



JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeOpChangePassphrase(JNIEnv* env,
        jobject self,
        jlong context,
        jlong keydata)
{
    gpgme_error_t err;

    err = gpgme_op_passwd(CONTEXT(context), KEY(keydata), 0);
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }
}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeRelease(JNIEnv* env,
        jobject self,
        jlong context)
{
    gpgme_release(CONTEXT(context));
}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeOpEncryptSign(JNIEnv* env,
        jobject self,
        jlong context,
        jlongArray recipients,
        jlong plain,
        jlong cipher)
{
    gpgme_error_t err;

    jsize len = (*env)->GetArrayLength(env, recipients);
    gpgme_key_t keys[len + 1];
    if (UTILS_copyRecipientsFromJvm(env, recipients, keys) < 1) {
        return;
    }

    err = gpgme_data_rewind(DATA(plain));   //TODO: Use seek instead of rewind
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }

    err = gpgme_data_rewind(DATA(cipher));   //TODO: Use seek instead of rewind
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }

    err = gpgme_op_encrypt_sign(CONTEXT(context), keys,
                                GPGME_ENCRYPT_ALWAYS_TRUST, DATA(plain),
                                DATA(cipher));
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }
    /*   err = gpgme_op_encrypt_sign_start(CONTEXT(context), keys, GPGME_ENCRYPT_ALWAYS_TRUST, (gpgme_data_t)plain, (gpgme_data_t)cipher); */
    /*   if(UTILS_onErrorThrowException(env, err)){ */
    /*     return; */
    /*   } */
    /*   gpgme_ctx_t waitedOn = gpgme_wait(CONTEXT(context), err, 1);//HANG UNTIL COMPLETED! */
    /*   if(waitedOn != NULL && UTILS_onErrorThrowException(env, err)){ */
    /*     return; */
    /*   } */
}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeOpDecryptVerify(JNIEnv* env,
        jobject self,
        jlong context,
        jlong cipher,
        jlong plain)
{
    gpgme_error_t err;

    err = gpgme_data_rewind(DATA(cipher));   //TODO: Use seek instead of rewind
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }

    err = gpgme_data_rewind(DATA(plain));   //TODO: Use seek instead of rewind
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }

    err = gpgme_op_decrypt_verify(CONTEXT(context), DATA(cipher), DATA(plain));
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }
    /*   err = gpgme_op_decrypt_verify_start(CONTEXT(context), (gpgme_data_t)cipher, (gpgme_data_t)plain); */
    /*   if(UTILS_onErrorThrowException(env, err)){ */
    /*     return; */
    /*   } */
    /*   gpgme_ctx_t waitedOn = gpgme_wait(CONTEXT(context), err, 1);//HANG UNTIL COMPLETED! */
    /*   if(waitedOn != NULL && UTILS_onErrorThrowException(env, err)){ */
    /*     return; */
    /*   } */
}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeOpSign(JNIEnv* env,
        jobject self,
        jlong context,
        jlong plain,
        jlong signature)
{
    gpgme_error_t err;

    err = gpgme_data_rewind(DATA(plain));   //TODO: Use seek instead of rewind
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }

    err = gpgme_data_rewind(DATA(signature));   //TODO: Use seek instead of rewind
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }

    err = gpgme_op_sign(CONTEXT(context), DATA(plain), DATA(signature),
                        GPGME_SIG_MODE_CLEAR);
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }
    /*   err = gpgme_op_sign_start(CONTEXT(context), (gpgme_data_t)plain, (gpgme_data_t)signature, GPGME_SIG_MODE_NORMAL); */
    /*   if(UTILS_onErrorThrowException(env, err)){ */
    /*     return; */
    /*   } */

    /*   gpgme_ctx_t waitedOn = gpgme_wait(CONTEXT(context), err, 0); */
    /*   if(waitedOn != NULL && UTILS_onErrorThrowException(env, err)){ */
    /*     return; */
    /*   } */

}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeOpVerify(JNIEnv* env,
        jobject self,
        jlong context,
        jlong signature,
        jlong signedtxt,
        jlong plain)
{
    gpgme_error_t err;

    err = gpgme_data_rewind(DATA(signature));   //TODO: Use seek instead of rewind
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }

    err = gpgme_data_rewind(DATA(signedtxt));   //TODO: Use seek instead of rewind
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }

    err = gpgme_data_rewind(DATA(plain));   //TODO: Use seek instead of rewind
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }

    err = gpgme_op_verify(CONTEXT(context), DATA(signature), DATA(signedtxt),
                          DATA(plain));
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }
    /*   err = gpgme_op_verify_start(CONTEXT(context), (gpgme_data_t)signature, (gpgme_data_t)signedtxt, (gpgme_data_t)plain); */
    /*   if(UTILS_onErrorThrowException(env, err)){ */
    /*     return; */
    /*   } */
    /*   gpgme_ctx_t waitedOn = gpgme_wait(CONTEXT(context), err, 0); */
    /*   if(waitedOn != NULL && UTILS_onErrorThrowException(env, err)){ */
    /*     return; */
    /*   } */
}

JNIEXPORT jobjectArray JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeKeylist(JNIEnv* env,
        jobject self,
        jlong context,
        jstring query,
        jboolean secret_only)
{
    //copy string object from java to native string
    const jsize query_len = (*env)->GetStringLength(env, query);
    const jbyte* query_str = (jbyte*)(*env)->GetStringUTFChars(env, query,
                             NULL);
    //get the right constructor to invoke for every key in result set
    jclass keyClass;
    keyClass = (*env)->FindClass(env, "com/freiheit/gnupg/GnuPGKey");
    if (keyClass == NULL) {
        return NULL;
    }
    jmethodID cid;
    //get the constructor, that accepts a long as param (that is a ptr to a key)..
    cid = (*env)->GetMethodID(env, keyClass, "<init>", "(J)V");
    if (cid == NULL) {
        return NULL;
    }

    gpgme_error_t err = gpgme_op_keylist_start(CONTEXT(context),
                        query_len > 0 ? (const char*)query_str : NULL, secret_only);
    if (UTILS_onErrorThrowException(env, err)) {
        (*env)->ReleaseStringUTFChars(env, query, (const char*)query_str);
        return NULL;
    }

    gpgme_key_t key;
    jlong num_keys_found = 0;
    struct _keyInList {
        gpgme_key_t key;
        struct _keyInList* next;
    };
    typedef struct _keyInList* keyInList;
    keyInList current, next, head = NULL;
    while (!err) {
        err = gpgme_op_keylist_next(CONTEXT(context), &key);
        if ((gpg_err_code(err) != GPG_ERR_EOF)
                && UTILS_onErrorThrowException(env, err)) {
            return NULL;
        } else if (err) {
            break; // we have nothing, quit before setting the list
        }
        current = (keyInList)malloc(sizeof(keyInList));
        current->key = key;
        current->next = head;
        head = current;
        num_keys_found++;
    }
    current = head;
    jobject keyObj = NULL;
    jobjectArray result = (*env)->NewObjectArray(env,
                          num_keys_found,
                          keyClass,
                          NULL);
    int i;
    for (i = 0; i < num_keys_found; i++) {
        key = current->key;
        keyObj = (*env)->NewObject(env, keyClass, cid, LNG(key));
        (*env)->SetObjectArrayElement(env, result, i, keyObj);
        next = current->next;
        free(current);
        current = next;
    }

    //..and release the query string for gc..
    (*env)->ReleaseStringUTFChars(env, query, (const char*) query_str);

    return result;
}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeAddSigners(JNIEnv* env,
        jobject self,
        jlong context,
        jlong key)
{
    gpgme_error_t err;
    err = gpgme_signers_add(CONTEXT(context), KEY(key));
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }
}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeClearSigners(JNIEnv* env,
        jobject self,
        jlong context)
{
    gpgme_signers_clear(CONTEXT(context));
}

JNIEXPORT jlong JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeGetSigner(JNIEnv* env,
        jobject self,
        jlong context,
        jint index)
{
    gpgme_key_t key = gpgme_signers_enum(CONTEXT(context), index);
    return LNG(key);
}

JNIEXPORT jlong JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeGetSignersLength(JNIEnv* env,
        jobject self,
        jlong context)
{
    return (gpgme_signers_count(CONTEXT(context)));
}

void check_result(gpgme_import_result_t result, char* fpr, int secret)
{
    //TODO: Throw exception for possible errors...
    if (result->considered != 1) {
        fprintf(stderr, "Unexpected number of considered keys %i\n",
                result->considered);
    }

    if (result->no_user_id != 0) {
        fprintf(stderr, "Unexpected number of user ids %i\n",
                result->no_user_id);
    }

    if ((secret && result->imported != 0)
            || (!secret && (result->imported != 0 && result->imported != 1))) {
        fprintf(stderr, "Unexpected number of imported keys %i\n",
                result->imported);
    }

    if (result->imported_rsa != 0) {
        fprintf(stderr, "Unexpected number of imported RSA keys %i\n",
                result->imported_rsa);
    }

    if ((secret && result->unchanged != 0)
            || (!secret && ((result->imported == 0 && result->unchanged != 1)
                            || (result->imported == 1 && result->unchanged != 0)))) {
        fprintf(stderr, "Unexpected number of unchanged keys %i\n",
                result->unchanged);
    }

    if (result->new_user_ids != 0) {
        fprintf(stderr, "Unexpected number of new user IDs %i\n",
                result->new_user_ids);
    }

    if (result->new_sub_keys != 0) {
        fprintf(stderr, "Unexpected number of new sub keys %i\n",
                result->new_sub_keys);
    }

    if ((secret
            && ((result->secret_imported == 0 && result->new_signatures != 0)
                || (result->secret_imported == 1 && result->new_signatures > 1)))
            || (!secret && result->new_signatures != 0)) {
        fprintf(stderr, "Unexpected number of new signatures %i\n",
                result->new_signatures);
        if (result->new_signatures == 2) {
            fprintf(stderr, "### ignored due to gpg 1.3.4 problems\n");
        } else {
            //exit (1);
        }
    }

    if (result->new_revocations != 0) {
        fprintf(stderr, "Unexpected number of new revocations %i\n",
                result->new_revocations);
    }

    if ((secret && result->secret_read != 1)
            || (!secret && result->secret_read != 0)) {
        fprintf(stderr, "Unexpected number of secret keys read %i\n",
                result->secret_read);
    }

    if ((secret && result->secret_imported != 0 && result->secret_imported != 1)
            || (!secret && result->secret_imported != 0)) {
        fprintf(stderr, "Unexpected number of secret keys imported %i\n",
                result->secret_imported);
    }

    if ((secret
            && ((result->secret_imported == 0 && result->secret_unchanged != 1)
                || (result->secret_imported == 1
                    && result->secret_unchanged != 0)))
            || (!secret && result->secret_unchanged != 0)) {
        fprintf(stderr, "Unexpected number of secret keys unchanged %i\n",
                result->secret_unchanged);
    }

    if (result->not_imported != 0) {
        fprintf(stderr, "Unexpected number of secret keys not imported %i\n",
                result->not_imported);
    }

    if (secret) {
        if (!result->imports
                || (result->imports->next && result->imports->next->next)) {
            fprintf(stderr, "Unexpected number of status reports\n");
        }
    } else if (!result->imports || result->imports->next) {
        fprintf(stderr, "Unexpected number of status reports\n");
    }

    if (strcmp(fpr, result->imports->fpr)) {
        fprintf(stderr, "Unexpected fingerprint %s\n", result->imports->fpr);
    }

    if (result->imports->next && strcmp(fpr, result->imports->next->fpr)) {
        fprintf(stderr, "Unexpected fingerprint on second status %s\n",
                result->imports->next->fpr);
    }

    if (result->imports->result != 0) {
        fprintf(stderr, "Unexpected status result %s\n",
                gpgme_strerror(result->imports->result));
    }

    if (secret) {
        if (result->secret_imported == 0) {
            if (result->imports->status != GPGME_IMPORT_SECRET) {
                fprintf(stderr, "Unexpected status %i\n",
                        result->imports->status);
            }
        } else if (result->imports->status
                   != (GPGME_IMPORT_SECRET | GPGME_IMPORT_NEW)
                   || (result->imports->next
                       && result->imports->next->status != GPGME_IMPORT_SIG)) {
            fprintf(stderr, "Unexpected status %i\n", result->imports->status);
        }
    } else if ((result->imported == 0 && result->imports->status != 0)
               || (result->imported == 1
                   && result->imports->status != GPGME_IMPORT_NEW)) {
        fprintf(stderr, "Unexpected status %i\n", result->imports->status);
    }
}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeOpImport(JNIEnv* env,
        jobject self,
        jlong context,
        jlong keydata)
{
    gpgme_error_t err;
    gpgme_import_result_t result;

    err = gpgme_data_rewind(DATA(keydata));   //TODO: Use seek instead of rewind
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }

    err = gpgme_op_import(CONTEXT(context), DATA(keydata));
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }
    result = gpgme_op_import_result(CONTEXT(context));
    if (result == NULL) {
        if (UTILS_onErrorThrowException(env, GPG_ERR_NO_PUBKEY))
            return;
    } else if (result->imported != 1 || result->not_imported != 0) {
        if (result->imports == NULL) {
            if (UTILS_onErrorThrowException(env, GPG_ERR_UNUSABLE_PUBKEY)) {
                return;
            }
        } else {
            if (UTILS_onErrorThrowException(env, result->imports->result)) {
                return;
            }
        }
    }
    //TODO: Check result and throw exceptions
    //check_result (result, "ADAB7FCC1F4DE2616ECFA402AF82244F9CD9FD55", 0);
}

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeGetArmor(JNIEnv* env,
        jobject self,
        jlong context)
{
    return (jboolean) gpgme_get_armor(CONTEXT(context));
}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeSetArmor(JNIEnv* env,
        jobject self,
        jlong context,
        jboolean armor_state)
{
    gpgme_set_armor(CONTEXT(context), (int) armor_state);
}

JNIEXPORT jboolean JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeGetTextmode(JNIEnv* env,
        jobject self,
        jlong context)
{
    return (jboolean) gpgme_get_textmode(CONTEXT(context));
}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeSetTextmode(JNIEnv* env,
        jobject self,
        jlong context,
        jboolean mode)
{
    gpgme_set_textmode(CONTEXT(context), (int) mode);
}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeOpGenKey(JNIEnv* env,
        jobject self,
        jlong context,
        jstring params)
{

    gpgme_ctx_t ctx = CONTEXT(context);
    char* p;
    gpgme_error_t err;

    p = (char*)(*env)->GetStringUTFChars(env, params, NULL);

    fprintf(stderr, "genKey: \"%s\"\n", p);

    err = gpgme_op_genkey(ctx, p, NULL, NULL);

    (*env)->ReleaseStringUTFChars(env, params, p);

    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }

}


JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeOpExport(JNIEnv* env,
        jobject self,
        jlong context,
        jstring pattern,
        jlong reserved,
        jlong keydata)
{

    gpgme_ctx_t ctx = CONTEXT(context);
    char* p;
    gpgme_data_t data = DATA(keydata);
    gpgme_error_t err;


    p = (char*)(*env)->GetStringUTFChars(env, pattern, NULL);
    err = gpgme_op_export(ctx, p, 0, data);
    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }
    (*env)->ReleaseStringUTFChars(env, pattern, p);
}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeCtxSetEngineInfo(JNIEnv* env,
        jobject self,
        jlong ctx,
        jint proto,
        jstring fileName,
        jstring homeDir)
{

    gpgme_ctx_t context = CONTEXT(ctx);
    gpgme_protocol_t protocol = (gpgme_protocol_t) proto;
    char* file_name;
    char* home_dir;
    gpgme_error_t err;

    file_name = (char*)(*env)->GetStringUTFChars(env, fileName, NULL);
    home_dir = (char*)(*env)->GetStringUTFChars(env, homeDir, NULL);


    /*fprintf(stderr, "set engine info: proto: %d, fileName: %s, homeDir: %s\n", proto, file_name, home_dir);*/
    err = gpgme_ctx_set_engine_info(context, protocol, file_name, home_dir);

    (*env)->ReleaseStringUTFChars(env, fileName, file_name);
    (*env)->ReleaseStringUTFChars(env, homeDir, home_dir);

    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }

}


JNIEXPORT jobject JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeOpGenkeyResult(JNIEnv* env,
        jobject self,
        jlong ctx)
{

    gpgme_ctx_t context = CONTEXT(ctx);
    gpgme_genkey_result_t result = gpgme_op_genkey_result(context);
    jobject resultObj;

    if (result == NULL) {
        return NULL;
    }
    //get the right constructor to invoke for every key in result set
    jclass resultClass;
    resultClass =
        (*env)->FindClass(env, "com/freiheit/gnupg/GnuPGGenkeyResult");

    if (resultClass == NULL) {
        return NULL;
    }

    jmethodID cid;
    cid = (*env)->GetMethodID(env, resultClass, "<init>", "()V");
    if (cid == NULL) {
        return NULL;
    }

    resultObj = (*env)->NewObject(env, resultClass, cid);

    UTILS_setStringMember(env, resultObj, resultClass, "_fpr", result->fpr);
    UTILS_setBooleanMember(env, resultObj, resultClass, "_primary",
                           result->primary);
    UTILS_setBooleanMember(env, resultObj, resultClass, "_sub", result->sub);

    return resultObj;
}

JNIEXPORT void JNICALL
Java_com_freiheit_gnupg_GnuPGContext_gpgmeOpDelete(JNIEnv* env,
        jobject self,
        jlong ctx,
        jlong key,
        jboolean allowSecret)
{

    gpgme_ctx_t context = CONTEXT(ctx);
    gpgme_key_t deletekey = KEY(key);
    unsigned int sec = (unsigned int) allowSecret;
    gpgme_error_t err;


    err = gpgme_op_delete(context, deletekey, sec);

    if (UTILS_onErrorThrowException(env, err)) {
        return;
    }
}

/*
 * Local Variables:
 * tab-width: 4;
 * c-basic-offset: 4;
 * c-file-style: k&r;
 * indent-tabs-mode: nil;
 * End:
 */
