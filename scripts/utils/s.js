import { create, env } from 'sanctuary'

const checkTypes = process.env.NODE_ENV !== 'production'

const S = create({checkTypes, env})

export default S
