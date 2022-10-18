#include <gpgme.h>
#include <stdio.h>
#include <stdlib.h>

/*
 * Test for the gpgme gen key function. This function seems
 * to hang from time to time.
 */

static int progress_called;

static void
progress (void *self, const char *what, int type, int current, int total)
{
  if (!strcmp (what, "primegen") && !current && !total
      && (type == '.' || type == '+' || type == '!'
          || type == '^' || type == '<' || type == '>'))
    {
      printf ("%c", type);
      fflush (stdout);
      progress_called = 1;
    }
  else
    {
      fprintf (stderr, "unknown progress `%s' %d %d %d\n", what, type,
               current, total);
      exit (1);
    }
}

int
main (int argc, char *argv[])
{
  gpgme_ctx_t ctx;
  gpgme_error_t err;
  char *p = "<GnupgKeyParms format=\"internal\">\n" \
            "Key-Type: DSA\n" \
            "Key-Length: 1024\n" \
            "Subkey-Type: ELG-E\n" \
            "Subkey-Length: 1024\n" \
            "Name-Real: alpha\n" \
            "Name-Comment: just a test\n" \
            "Name-Email: alpha@alpha.org\n" \
            "Expire-Date: 0\n" \
            "Passphrase: alpha\n" \
            "</GnupgKeyParms>";
  char *home = "/tmp/gnupg";

  if (argc > 1)
    {
      home = argv[1];
    }

  err = gpgme_new (&ctx);
  //fprintf(stderr, "gpgme_new, result: %d\n", err);
  //gpgme_set_armor (ctx, 1);
  //fprintf(stderr, "gpgme_set_armor, result: %d\n", err);
  //gpgme_set_textmode (ctx, 1);
  //fprintf(stderr, "gpgme_set_textmode, result: %d\n", err);
  //err = gpgme_set_keylist_mode (ctx, GPGME_KEYLIST_MODE_LOCAL | GPGME_KEYLIST_MODE_SIGS);
  //fprintf(stderr, "gpgme_set_keylist_mode, result: %d\n", err);
  fprintf (stderr, "set home to \"%s\"\n", home);
  err = gpgme_ctx_set_engine_info (ctx, 0, "/usr/bin/gpg", home);
  fprintf(stderr, "gpgme_set_engine_info, result: %d\n", err);

  fprintf(stderr, "starting genkey: \"%s\"\n", p);

  gpgme_set_progress_cb (ctx, progress, NULL);
  err = gpgme_op_genkey(ctx, p, NULL, NULL);

  fprintf(stderr, "done, result: %d\n", err);

  return 0;
}
