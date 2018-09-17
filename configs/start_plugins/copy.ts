import plugin from '@start/plugin'

export default (
  outDirRelative: string,
  options: {
    rename?: (targetFilePath: string) => string
  } = {},
) =>
  plugin('copy', async ({ files, logFile }) => {
    const path = await import('path')
    const { default: movePath } = await import('move-path')
    const { default: makeDir } = await import('make-dir')
    const { default: copie } = await import('copie')

    return {
      files: await Promise.all(
        (files || []).map(async (file) => {
          let outFile: string
          if (options.rename) {
            const outFileRelativePath = path.relative(process.cwd(), options.rename(file.path))
            const outFileContainer = path.resolve(process.cwd(), outDirRelative)
            outFile = path.resolve(outFileContainer, outFileRelativePath)
          } else {
            outFile = movePath(file.path, outDirRelative)
          }
          const outDir = path.dirname(outFile)

          await makeDir(outDir)
          await copie(file.path, outFile)

          logFile(outFile)

          return file
        })
      )
    }
  })
