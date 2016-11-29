const fs = require('fs')
const sysPath = require('path')

module.exports = function sassImporter(url, filePath, done) {
  const modulePath = sysPath.resolve(__dirname, '../node_modules') + '/'
  const targetPath = url.replace(/^~\/?/, modulePath)
  if (sysPath.extname(targetPath) === '.css') {
    fs.readFile(targetPath, (err, data) => {
      err ? done(err) : done({contents: data.toString()})
    })
  } else {
    done({file: targetPath})
  }
}
