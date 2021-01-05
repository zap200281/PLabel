import { KeyframeTrack } from '../KeyframeTrack.js';

/**
 *
 * A Track of keyframe values that represent color.
 *
 *
 * @author Ben Houston / http://clara.io/
 * @author David Sarno / http://lighthaus.us/
 * @author tschw
 */
 // {
 //       "type": "color",
 //       "name": "Box.material.color",
 //       "keys": [{
 //         "value": [1, 0, 0, 1],
 //         "time": 20
 //       }, {
 //         "value": [0, 1, 0, 1],
 //         "time": 50
 //       }, {
 //         "value": [0, 0, 1, 1],
 //         "time": 80
 //       }, {
 //         "value": [1, 0, 0, 1],
 //         "time": 110
 //       }, {
 //         "value": [0, 1, 0, 1],
 //         "time": 140
 //       }]
 //     }
function ColorKeyframeTrack( name, times, values, interpolation ) {

	KeyframeTrack.call( this, name, times, values, interpolation );

}

ColorKeyframeTrack.prototype = Object.assign( Object.create( KeyframeTrack.prototype ), {

	constructor: ColorKeyframeTrack,
// 导出json文件："type": "color",
	ValueTypeName: 'color'

	// ValueBufferType is inherited

	// DefaultInterpolation is inherited

	// Note: Very basic implementation and nothing special yet.
	// However, this is the place for color space parameterization.

} );

export { ColorKeyframeTrack };
