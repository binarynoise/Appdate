# Appdate

[![Build Status](https://travis-ci.com/binarynoise/Appdate.svg?branch=master)](https://travis-ci.com/binarynoise/Appdate)

Appdate looks for updates of open-source apps by scanning the website where the updates are released.

Updates are fetched when you open the app and every 4 hours when connected to wifi. (No setting for that yet)

Appdate also fetches a list of templates, so you don't need to input all the apps again manually. 
For now you need to open an issue if you want to add an app to the list of templates

## Example:  
 - Appdate is released at `github.com/binarynoise/Appdate/releases`.

**Important**: you need to point to the site where you can actually download the `.apk` 
as Appdate scans the `html` for links to these files. 

## currently supported
 - [x] github.com _public_ releases (and similar websites)
 - [x] Xposed repository
 - [x] AndroidFileHost (I worked myself into the api :wink:, but it's a little bit slow)
 - [ ] websites that use relative links to their files (and also ftp)
 - [ ] more to come...

_Note_: please double-check if the version found by Appdate is the same as you'd expect because some developers use 
a different versioning (like xposed) in their filenames than in their app.  
Example: version in app manifest `v2.5.3`, version in filename `253`
Open an issue and I'll add this site to the ones parsed differently.

## License
see [`License.md`](LICENSE.md) for license details
