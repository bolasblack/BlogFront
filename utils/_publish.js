import R from 'ramda'
import S from '../scripts/utils/s'
import fs from 'fs'
import path from 'path'
import chalk from 'chalk'
import childProcess from 'child_process'

const projectRoot = path.resolve(__dirname, '../')
const repoPath = path.resolve(projectRoot, 'publish_repo')

// :: String -> {String: String} -> String
const fillStr = R.curry((template, data) => {
  return template.replace(/%{([^}]+)}/, ($0, $1) => data[$1] || $0)
})

// :: String -> Object -> Either(Error, Undefined)
const exec = R.curry((cmd, options) => {
  try {
    console.log(`> ${chalk.green(cmd)}`)
    childProcess.execSync(cmd, options || {stdio: 'inherit'})
    return S.Right(cmd)
  } catch (err) {
    return S.Left(err)
  }
})

// :: String -> Either(Error, JSON)
const readJSONFile = S.encaseEither(
  S.I,
  R.compose(
    JSON.parse,
    ::fs.readFileSync,
  ),
)

// :: String -> Either(Error, Undefined)
const cloneRepoByGithubPath = R.ifElse(
  R.partial(::fs.existsSync, [repoPath]),
  R.nAry(0, S.Right),
  R.pipe(
    R.unapply(fillStr(`git clone git@github.com:%{0}.git ${repoPath}`)),
    exec(R.__, null),
  ),
)

export default () => {
  const result = readJSONFile(`${projectRoot}/package.json`)
    .map(R.prop('githubBlogRepo'))
    .chain(cloneRepoByGithubPath)
    .chain(() => {
      return R.reduce((memo, cmd) => {
        return memo.isLeft ? R.reduced(memo) : exec(cmd, null)
      }, S.Right(), [
        `rm -rf "${projectRoot}/public"`,
        `cd ${projectRoot} && ${projectRoot}/node_modules/.bin/gulp build`,
        `cd "${repoPath}" && git checkout gh-pages`,
        `rm -rf "${repoPath}/"*`,
        `cp -r "${projectRoot}/public/"* "${repoPath}/"`,
        `cd "${repoPath}" && git add -A && git commit -m "${new Date().toJSON()}" && git push origin gh-pages`,
      ])
    })

  S.either(
    R.compose(::console.error, R.concat('\n'), R.prop('stack')),
    S.I,
    result,
  )
}
