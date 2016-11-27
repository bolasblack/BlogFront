import GitHub from 'github-api'

const gh = new GitHub()

export default gh
export const repo = gh.getRepo('bolasblack', 'BlogPosts')
