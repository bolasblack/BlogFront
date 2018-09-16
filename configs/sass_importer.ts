import fs from 'fs'
import path from 'path'

export function sassImporter(
  url: string,
  filePath: string,
  done: (result: Error | { file: string } | { contents: string }) => void,
) {
  const modulePath = path.resolve(__dirname, '../node_modules') + '/'
  const targetPath = url.replace(/^~\/?/, modulePath)
  if (path.extname(targetPath) === '.css') {
    fs.readFile(targetPath, (err, data) => {
      err ? done(err) : done({ contents: data.toString() })
    })
  } else {
    done({ file: targetPath })
  }
}
