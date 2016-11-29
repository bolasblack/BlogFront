import Immutable from 'immutable'
import R from 'ramda'

const ANONYMOUS = '<<anonymous>>'

createChainableTypeChecker.Date = createChainableTypeChecker((props, propName, componentName) => {
  if ( !(props[propName] instanceof Date) ) {
    return new Error(`Invalid prop \`${propName}\` supplied to \`${componentName}\`. It should be Date.`)
  }
})

createChainableTypeChecker.Immutable = createChainableTypeChecker((props, propName, componentName) => {
  if (!Immutable.Iterable.isIterable(props[propName])) {
    return new Error(`Invalid prop \`${propName}\` supplied to \`${componentName}\`. It should be Immutable.`)
  }
})
;['List', 'Map', 'OrderedMap', 'Set', 'OrderedSet', 'Stack', 'Record', 'Seq'].forEach((immutableType) => {
  createChainableTypeChecker.Immutable[immutableType] = createChainableTypeChecker((props, propName, componentName) => {
    if (!Immutable[immutableType][`is${immutableType}`](props[propName])) {
      return new Error(`Invalid prop \`${propName}\` supplied to \`${componentName}\`. It should be Immutable.${immutableType} .`)
    }
  })
})

// Pulled from:
//    https://github.com/facebook/react/blob/master/src/isomorphic/classic/types/ReactPropTypes.js#L108
// and
//    https://github.com/jackrzhang/react-custom-proptypes/blob/master/lib/custom-proptypes.js#L4
export default function createChainableTypeChecker(validate) {
  function checkType(
    isRequired,
    props,
    propName,
    componentName = ANONYMOUS
  ) {
    if (props[propName] == null) {
      if (isRequired) {
        if (props[propName] === null) {
          return new Error(
            `The \`${propName}\` is marked as required ` +
            `in \`${componentName}\`, but its value is \`null\`.`
          );
        }

        return new Error(
          `The \`${propName}\` is marked as required in ` +
          `\`${componentName}\`, but its value is \`undefined\`.`
        );
      }

      return null;
    }

    return validate(
      props,
      propName,
      componentName
    );
  }

  // Use of .bind for creating the chainable `.isRequired` interface
  const chainedCheckType = checkType.bind(null, false);
  chainedCheckType.isRequired = checkType.bind(null, true);
  chainedCheckType.validate = validate;

  return chainedCheckType;
}
