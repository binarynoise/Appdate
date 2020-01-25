# Appdate

[![Build Status](https://travis-ci.com/binarynoise/Appdate.svg?branch=master)](https://travis-ci.com/binarynoise/Appdate)
[![GitHub stars](https://img.shields.io/github/stars/binarynoise/Appdate.svg?style=social&maxAge=2592000)](https://GitHub.com/binarynoise/Appdate/stargazers)
[![GitHub releases](https://img.shields.io/github/release/binarynoise/Appdate.svg)](https://GitHub.com/binarynoise/Appdate/releases/latest/)
[![GitHub issues](https://img.shields.io/github/issues/binarynoise/Appdate.svg)](https://GitHub.com/binarynoise/Appdate/issues/)

[![built-for-android](https://forthebadge.com/images/badges/built-for-android.svg)](https://forthebadge.com)

Appdate looks for updates of open-source apps by scanning the website
where the updates are released.

Updates are fetched when you open the app and every 4 hours when
connected to Wi-Fi. (No setting for that yet)

Allows direct installation after successful download (see preferences).

Apks can be installed with and without root:
 - with root: uses `pm install`, silent install
 - without root: uses `PackageManager`, requires user interaction

Appdate also fetches a list of templates, so you don't need to input all
the apps again manually. For now, you need to open an issue if you want
to add an app to the list of templates.

## Downloads

- [here at github](https://github.com/binarynoise/Appdate/releases)
- [at androidfilehost](https://www.androidfilehost.com/?w=files&flid=304124&sort_by=date&sort_dir=DESC)  
  here you can find the latest builds as I am too lazy to seperate the
  changes and update the git repo
- [debug builds at afh](https://www.androidfilehost.com/?w=files&flid=304125&sort_by=date&sort_dir=DESC)
- [at onedrive (release + debug)](https://1drv.ms/u/s!AoZBVRELG0nWc67mGUIalpTtEwI?e=tia9Fh)  
  also latest builds but without version in file name

## Example:

- Appdate is released at `github.com/binarynoise/Appdate/releases`.

**Important**: you need to point to the site where you can actually
download the `.apk` as Appdate scans the `html` for links to these
files, e.g. for github you need to append `/releases`

## currently supported

- [x] github.com _public_ releases (and similar websites)
- [x] Xposed repository
- [x] AndroidFileHost (I worked myself into the api :wink:, but it's a
      little bit slow)
- [ ] websites that use relative links to their files (and also ftp)
- [ ] more to come...

_Note_: please double-check if the version found by Appdate is the same
as you'd expect because some developers use a different versioning (like
xposed) in their filenames than in their app. Example: version in app
manifest `v2.5.3`, version in filename `253` Open an issue and I'll add
this site to the ones parsed differently.

## License

see [`License.md`](LICENSE.md) for license details
