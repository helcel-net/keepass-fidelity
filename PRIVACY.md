# Privacy Policy for Keepass Fidelity

**App:** Keepass Fidelity (`net.helcel.fidelity`)
**Developer:** Helcel
**Effective date:** 19 September 2026

## What we collect

Nothing. We operate no servers and receive no data from the app. Keepass
Fidelity has no internet access, no account, and contains no analytics,
advertising, or tracking libraries.

## Your data

Your loyalty cards (names and barcodes) are stored in the
KeePass database you choose, not in a separate store controlled by us:

- **Standalone mode:** the app opens a `.kdbx` file you select, using the
  bundled KeePassDX engine. The file stays where you put it, encrypted by
  KeePass with your password or key file.
- **Keepass2Android plugin mode:** entries are read and created through the
  Keepass2Android app installed on your device. That app holds the database and
  its own privacy policy applies to it.

Your database password is used only to unlock your database on the device. It is
never stored anywhere by us and never leaves your device.

If your database file is kept in a folder synchronised by another app or cloud
service, that service handles the file under its own privacy policy.

## On-device cache

For quick access, the app keeps a recently-used history and may cache entry data
in its private storage. You can mark individual entries to be excluded from
caching, and the cache can be cleared from the app or by clearing the app's data
in Android Settings.

## Permissions

- `CAMERA` — scanning a barcode to create an entry. Camera frames are processed
  on the device and are not stored or transmitted.
- `READ_MEDIA_VISUAL_USER_SELECTED` — reading a barcode from an image you
  explicitly select. Only the images you pick are accessed, and only while
  importing.

## Retention and deletion

Cached data and history remain until you clear them or uninstall the app.
Uninstalling does not delete your KeePass database, which is your own file:
delete it yourself, or remove entries through the app or any KeePass client.

## Children

Keepass Fidelity is suitable for all ages and collects no data from any user,
including children.

## Security

Your cards are protected by KeePass encryption, which depends on the strength of
the password or key file you choose. On-device cache and history are held in the
app's private storage, protected by the Android sandbox.

## Open source

Keepass Fidelity is released into the public domain under the Unlicense. The
full source is available at https://github.com/helcel-net/keepass-fidelity, so
these statements can be independently verified.

## Changes

Any change affecting privacy will be published in this document before or with
the release that introduces it.

## Contact

Email: net.helcel+privacy@google.com
Issues: https://github.com/helcel-net/keepass-fidelity/issues