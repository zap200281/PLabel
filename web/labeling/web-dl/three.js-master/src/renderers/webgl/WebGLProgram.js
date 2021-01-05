/**
 * @author mrdoob / http://mrdoob.com/
 */

import { WebGLUniforms } from './WebGLUniforms.js';
import { WebGLShader } from './WebGLShader.js';
import { ShaderChunk } from '../shaders/ShaderChunk.js';
import { NoToneMapping, AddOperation, MixOperation, MultiplyOperation, EquirectangularRefractionMapping, CubeRefractionMapping, SphericalReflectionMapping, EquirectangularReflectionMapping, CubeUVRefractionMapping, CubeUVReflectionMapping, CubeReflectionMapping, PCFSoftShadowMap, PCFShadowMap, CineonToneMapping, Uncharted2ToneMapping, ReinhardToneMapping, LinearToneMapping, GammaEncoding, RGBDEncoding, RGBM16Encoding, RGBM7Encoding, RGBEEncoding, sRGBEncoding, LinearEncoding } from '../../constants.js';

var programIdCount = 0;

function getEncodingComponents( encoding ) {

	switch ( encoding ) {

		case LinearEncoding:
			return [ 'Linear', '( value )' ];
		case sRGBEncoding:
			return [ 'sRGB', '( value )' ];
		case RGBEEncoding:
			return [ 'RGBE', '( value )' ];
		case RGBM7Encoding:
			return [ 'RGBM', '( value, 7.0 )' ];
		case RGBM16Encoding:
			return [ 'RGBM', '( value, 16.0 )' ];
		case RGBDEncoding:
			return [ 'RGBD', '( value, 256.0 )' ];
		case GammaEncoding:
			return [ 'Gamma', '( value, float( GAMMA_FACTOR ) )' ];
		default:
			throw new Error( 'unsupported encoding: ' + encoding );

	}

}

function getTexelDecodingFunction( functionName, encoding ) {

	var components = getEncodingComponents( encoding );
	return 'vec4 ' + functionName + '( vec4 value ) { return ' + components[ 0 ] + 'ToLinear' + components[ 1 ] + '; }';

}

function getTexelEncodingFunction( functionName, encoding ) {

	var components = getEncodingComponents( encoding );
	return 'vec4 ' + functionName + '( vec4 value ) { return LinearTo' + components[ 0 ] + components[ 1 ] + '; }';

}

function getToneMappingFunction( functionName, toneMapping ) {

	var toneMappingName;

	switch ( toneMapping ) {

		case LinearToneMapping:
			toneMappingName = 'Linear';
			break;

		case ReinhardToneMapping:
			toneMappingName = 'Reinhard';
			break;

		case Uncharted2ToneMapping:
			toneMappingName = 'Uncharted2';
			break;

		case CineonToneMapping:
			toneMappingName = 'OptimizedCineon';
			break;

		default:
			throw new Error( 'unsupported toneMapping: ' + toneMapping );

	}

	return 'vec3 ' + functionName + '( vec3 color ) { return ' + toneMappingName + 'ToneMapping( color ); }';

}

function generateExtensions( extensions, parameters, rendererExtensions ) {

	extensions = extensions || {};

	var chunks = [
		( extensions.derivatives || parameters.envMapCubeUV || parameters.bumpMap || parameters.normalMap || parameters.flatShading ) ? '#extension GL_OES_standard_derivatives : enable' : '',
		( extensions.fragDepth || parameters.logarithmicDepthBuffer ) && rendererExtensions.get( 'EXT_frag_depth' ) ? '#extension GL_EXT_frag_depth : enable' : '',
		( extensions.drawBuffers ) && rendererExtensions.get( 'WEBGL_draw_buffers' ) ? '#extension GL_EXT_draw_buffers : require' : '',
		( extensions.shaderTextureLOD || parameters.envMap ) && rendererExtensions.get( 'EXT_shader_texture_lod' ) ? '#extension GL_EXT_shader_texture_lod : enable' : ''
	];

	return chunks.filter( filterEmptyLine ).join( '\n' );

}

function generateDefines( defines ) {

	var chunks = [];

	for ( var name in defines ) {

		var value = defines[ name ];

		if ( value === false ) continue;

		chunks.push( '#define ' + name + ' ' + value );

	}

	return chunks.join( '\n' );

}

function fetchAttributeLocations( gl, program ) {

	var attributes = {};

	var n = gl.getProgramParameter( program, gl.ACTIVE_ATTRIBUTES );

	for ( var i = 0; i < n; i ++ ) {

		var info = gl.getActiveAttrib( program, i );
		var name = info.name;

		// console.log( 'THREE.WebGLProgram: ACTIVE VERTEX ATTRIBUTE:', name, i );

		attributes[ name ] = gl.getAttribLocation( program, name );

	}

	return attributes;

}

function filterEmptyLine( string ) {

	return string !== '';

}
// 替换灯的数量
function replaceLightNums( string, parameters ) {
// NUM_DIR_LIGHTS等字符出现在了着色器代码中，但是不属于预定义，可以理解为自动化编写生成代码   或者说自动化设置着色器的代码是否执行
// 与光照相关的判断条件   只有光照的数量大于0才可以执行相应的程序   threejs可以递归统计光照的数量
	return string
		.replace( /NUM_DIR_LIGHTS/g, parameters.numDirLights )
		.replace( /NUM_SPOT_LIGHTS/g, parameters.numSpotLights )
		.replace( /NUM_RECT_AREA_LIGHTS/g, parameters.numRectAreaLights )
		.replace( /NUM_POINT_LIGHTS/g, parameters.numPointLights )
		.replace( /NUM_HEMI_LIGHTS/g, parameters.numHemiLights );

}

function replaceClippingPlaneNums( string, parameters ) {

	return string
		.replace( /NUM_CLIPPING_PLANES/g, parameters.numClippingPlanes )
		.replace( /UNION_CLIPPING_PLANES/g, ( parameters.numClippingPlanes - parameters.numClipIntersection ) );

}
// parse:解析   解析Includes：            #include <common>   string：顶点、片元着色器代码
function parseIncludes( string ) {

	var pattern = /^[ \t]*#include +<([\w\d.]+)>/gm;

	function replace( match, include ) {

		var replace = ShaderChunk[ include ];

		if ( replace === undefined ) {

			throw new Error( 'Can not resolve #include <' + include + '>' );

		}

		return parseIncludes( replace );

	}
// 参数string，也就是着色器代码字符串，执行replace方法
	return string.replace( pattern, replace );

}

function unrollLoops( string ) {

	var pattern = /#pragma unroll_loop[\s]+?for \( int i \= (\d+)\; i < (\d+)\; i \+\+ \) \{([\s\S]+?)(?=\})\}/g;

	function replace( match, start, end, snippet ) {

		var unroll = '';

		for ( var i = parseInt( start ); i < parseInt( end ); i ++ ) {

			unroll += snippet.replace( /\[ i \]/g, '[ ' + i + ' ]' );

		}

		return unroll;

	}

	return string.replace( pattern, replace );

}

function WebGLProgram( renderer, extensions, code, material, shader, parameters ) {
// 上下文
	var gl = renderer.context;
// 预定义
	var defines = material.defines;
// 顶点着色器、片元着色器代码
	var vertexShader = shader.vertexShader;
	var fragmentShader = shader.fragmentShader;

	var shadowMapTypeDefine = 'SHADOWMAP_TYPE_BASIC';

	if ( parameters.shadowMapType === PCFShadowMap ) {

		shadowMapTypeDefine = 'SHADOWMAP_TYPE_PCF';

	} else if ( parameters.shadowMapType === PCFSoftShadowMap ) {

		shadowMapTypeDefine = 'SHADOWMAP_TYPE_PCF_SOFT';

	}

	var envMapTypeDefine = 'ENVMAP_TYPE_CUBE';
	var envMapModeDefine = 'ENVMAP_MODE_REFLECTION';
	var envMapBlendingDefine = 'ENVMAP_BLENDING_MULTIPLY';

	if ( parameters.envMap ) {

		switch ( material.envMap.mapping ) {

			case CubeReflectionMapping:
			case CubeRefractionMapping:
				envMapTypeDefine = 'ENVMAP_TYPE_CUBE';
				break;

			case CubeUVReflectionMapping:
			case CubeUVRefractionMapping:
				envMapTypeDefine = 'ENVMAP_TYPE_CUBE_UV';
				break;

			case EquirectangularReflectionMapping:
			case EquirectangularRefractionMapping:
				envMapTypeDefine = 'ENVMAP_TYPE_EQUIREC';
				break;

			case SphericalReflectionMapping:
				envMapTypeDefine = 'ENVMAP_TYPE_SPHERE';
				break;

		}

		switch ( material.envMap.mapping ) {

			case CubeRefractionMapping:
			case EquirectangularRefractionMapping:
				envMapModeDefine = 'ENVMAP_MODE_REFRACTION';
				break;

		}

		switch ( material.combine ) {

			case MultiplyOperation:
				envMapBlendingDefine = 'ENVMAP_BLENDING_MULTIPLY';
				break;

			case MixOperation:
				envMapBlendingDefine = 'ENVMAP_BLENDING_MIX';
				break;

			case AddOperation:
				envMapBlendingDefine = 'ENVMAP_BLENDING_ADD';
				break;

		}

	}

	var gammaFactorDefine = ( renderer.gammaFactor > 0 ) ? renderer.gammaFactor : 1.0;

	// console.log( 'building new program ' );

	//

	var customExtensions = generateExtensions( material.extensions, parameters, extensions );

	var customDefines = generateDefines( defines );

	//
// 创建程序对象
	var program = gl.createProgram();

	var prefixVertex, prefixFragment;
// 判断是否是原始着色器材质对象
	if ( material.isRawShaderMaterial ) {

		prefixVertex = [

			customDefines

		].filter( filterEmptyLine ).join( '\n' );

		if ( prefixVertex.length > 0 ) {

			prefixVertex += '\n';

		}

		prefixFragment = [

			customExtensions,
			customDefines

		].filter( filterEmptyLine ).join( '\n' );

		if ( prefixFragment.length > 0 ) {

			prefixFragment += '\n';

		}

	} else {
// prefix前缀    下面的代码基本是Lambert材质顶点着色器#define LAMBERT之前的代码   难道这些代码是通用代码   比如一个预定义量表示自定义材质的名字、通用模型视图投影矩阵

// Lambert材质生成的代码：下面备注在响应的代码后面
// 采用三元运算符设置的，如果材质不具有响应的属性，不会生成代码，也就是空的字符串

		prefixVertex = [



			'precision ' + parameters.precision + ' float;',// precision highp float;
			'precision ' + parameters.precision + ' int;',// precision highp int;

			'#define SHADER_NAME ' + shader.name,// #define SHADER_NAME MeshLambertMaterial

			customDefines,//自定义的预定义   应该指的是  着色器的defines属性？

			parameters.supportsVertexTextures ? '#define VERTEX_TEXTURES' : '',//#define VERTEX_TEXTURES

			'#define GAMMA_FACTOR ' + gammaFactorDefine,//#define GAMMA_FACTOR 2

			'#define MAX_BONES ' + parameters.maxBones,//#define MAX_BONES 0
			( parameters.useFog && parameters.fog ) ? '#define USE_FOG' : '',
			( parameters.useFog && parameters.fogExp ) ? '#define FOG_EXP2' : '',

			parameters.map ? '#define USE_MAP' : '',
			parameters.envMap ? '#define USE_ENVMAP' : '',
			parameters.envMap ? '#define ' + envMapModeDefine : '',
			parameters.lightMap ? '#define USE_LIGHTMAP' : '',
			parameters.aoMap ? '#define USE_AOMAP' : '',
			parameters.emissiveMap ? '#define USE_EMISSIVEMAP' : '',
			parameters.bumpMap ? '#define USE_BUMPMAP' : '',
			parameters.normalMap ? '#define USE_NORMALMAP' : '',
			parameters.displacementMap && parameters.supportsVertexTextures ? '#define USE_DISPLACEMENTMAP' : '',
			parameters.specularMap ? '#define USE_SPECULARMAP' : '',
			parameters.roughnessMap ? '#define USE_ROUGHNESSMAP' : '',
			parameters.metalnessMap ? '#define USE_METALNESSMAP' : '',
			parameters.alphaMap ? '#define USE_ALPHAMAP' : '',
			// parameters.vertexColors如果是未定义  null  false  最后不会生成任何代码
			parameters.vertexColors ? '#define USE_COLOR' : '',//#define USE_COLOR    材质属性设置了顶点渲染   如果没设置顶点渲染：着色器代码中不会出现带预定义

			parameters.flatShading ? '#define FLAT_SHADED' : '',//#define FLAT_SHADED   是否平面着色

			parameters.skinning ? '#define USE_SKINNING' : '',
			parameters.useVertexTexture ? '#define BONE_TEXTURE' : '',//#define BONE_TEXTURE

			parameters.morphTargets ? '#define USE_MORPHTARGETS' : '',
			parameters.morphNormals && parameters.flatShading === false ? '#define USE_MORPHNORMALS' : '',
			parameters.doubleSided ? '#define DOUBLE_SIDED' : '',
			parameters.flipSided ? '#define FLIP_SIDED' : '',

			parameters.shadowMapEnabled ? '#define USE_SHADOWMAP' : '',
			parameters.shadowMapEnabled ? '#define ' + shadowMapTypeDefine : '',

			parameters.sizeAttenuation ? '#define USE_SIZEATTENUATION' : '',

			parameters.logarithmicDepthBuffer ? '#define USE_LOGDEPTHBUF' : '',
			parameters.logarithmicDepthBuffer && extensions.get( 'EXT_frag_depth' ) ? '#define USE_LOGDEPTHBUF_EXT' : '',

			// 预定义的部分，差异比较大   是否顶点渲染   是否平面着色  是否有各种纹理贴图   是否阴影   是都使用雾

			// 传递变量的部分，基本所有材质都一样
			//

			'uniform mat4 modelMatrix;',
			'uniform mat4 modelViewMatrix;',
			'uniform mat4 projectionMatrix;',
			'uniform mat4 viewMatrix;',
			'uniform mat3 normalMatrix;',
			'uniform vec3 cameraPosition;',

			'attribute vec3 position;',
			'attribute vec3 normal;',
			'attribute vec2 uv;',

			'#ifdef USE_COLOR',

			'	attribute vec3 color;',

			'#endif',

			'#ifdef USE_MORPHTARGETS',

			'	attribute vec3 morphTarget0;',
			'	attribute vec3 morphTarget1;',
			'	attribute vec3 morphTarget2;',
			'	attribute vec3 morphTarget3;',

			'	#ifdef USE_MORPHNORMALS',

			'		attribute vec3 morphNormal0;',
			'		attribute vec3 morphNormal1;',
			'		attribute vec3 morphNormal2;',
			'		attribute vec3 morphNormal3;',

			'	#else',

			'		attribute vec3 morphTarget4;',
			'		attribute vec3 morphTarget5;',
			'		attribute vec3 morphTarget6;',
			'		attribute vec3 morphTarget7;',

			'	#endif',

			'#endif',

			'#ifdef USE_SKINNING',

			'	attribute vec4 skinIndex;',
			'	attribute vec4 skinWeight;',

			'#endif',

			'\n'

		].filter( filterEmptyLine ).join( '\n' );
// 片元着色器代码前缀
		prefixFragment = [

			customExtensions,
// 片元精度定义
			'precision ' + parameters.precision + ' float;',
			'precision ' + parameters.precision + ' int;',

			'#define SHADER_NAME ' + shader.name,

			customDefines,

			parameters.alphaTest ? '#define ALPHATEST ' + parameters.alphaTest : '',

			'#define GAMMA_FACTOR ' + gammaFactorDefine,

			( parameters.useFog && parameters.fog ) ? '#define USE_FOG' : '',
			( parameters.useFog && parameters.fogExp ) ? '#define FOG_EXP2' : '',

			parameters.map ? '#define USE_MAP' : '',
			parameters.envMap ? '#define USE_ENVMAP' : '',
			parameters.envMap ? '#define ' + envMapTypeDefine : '',
			parameters.envMap ? '#define ' + envMapModeDefine : '',
			parameters.envMap ? '#define ' + envMapBlendingDefine : '',
			parameters.lightMap ? '#define USE_LIGHTMAP' : '',
			parameters.aoMap ? '#define USE_AOMAP' : '',
			parameters.emissiveMap ? '#define USE_EMISSIVEMAP' : '',
			parameters.bumpMap ? '#define USE_BUMPMAP' : '',
			parameters.normalMap ? '#define USE_NORMALMAP' : '',
			parameters.specularMap ? '#define USE_SPECULARMAP' : '',
			parameters.roughnessMap ? '#define USE_ROUGHNESSMAP' : '',
			parameters.metalnessMap ? '#define USE_METALNESSMAP' : '',
			parameters.alphaMap ? '#define USE_ALPHAMAP' : '',
			parameters.vertexColors ? '#define USE_COLOR' : '',

			parameters.gradientMap ? '#define USE_GRADIENTMAP' : '',

			parameters.flatShading ? '#define FLAT_SHADED' : '',

			parameters.doubleSided ? '#define DOUBLE_SIDED' : '',
			parameters.flipSided ? '#define FLIP_SIDED' : '',

			parameters.shadowMapEnabled ? '#define USE_SHADOWMAP' : '',
			parameters.shadowMapEnabled ? '#define ' + shadowMapTypeDefine : '',

			parameters.premultipliedAlpha ? '#define PREMULTIPLIED_ALPHA' : '',

			parameters.physicallyCorrectLights ? '#define PHYSICALLY_CORRECT_LIGHTS' : '',

			parameters.logarithmicDepthBuffer ? '#define USE_LOGDEPTHBUF' : '',
			parameters.logarithmicDepthBuffer && extensions.get( 'EXT_frag_depth' ) ? '#define USE_LOGDEPTHBUF_EXT' : '',

			parameters.envMap && extensions.get( 'EXT_shader_texture_lod' ) ? '#define TEXTURE_LOD_EXT' : '',
// 视图矩阵   相机矩阵
			'uniform mat4 viewMatrix;',
			'uniform vec3 cameraPosition;',

			( parameters.toneMapping !== NoToneMapping ) ? '#define TONE_MAPPING' : '',
			( parameters.toneMapping !== NoToneMapping ) ? ShaderChunk[ 'tonemapping_pars_fragment' ] : '', // this code is required here because it is used by the toneMapping() function defined below
			( parameters.toneMapping !== NoToneMapping ) ? getToneMappingFunction( 'toneMapping', parameters.toneMapping ) : '',

			parameters.dithering ? '#define DITHERING' : '',

			( parameters.outputEncoding || parameters.mapEncoding || parameters.envMapEncoding || parameters.emissiveMapEncoding ) ? ShaderChunk[ 'encodings_pars_fragment' ] : '', // this code is required here because it is used by the various encoding/decoding function defined below
			parameters.mapEncoding ? getTexelDecodingFunction( 'mapTexelToLinear', parameters.mapEncoding ) : '',
			parameters.envMapEncoding ? getTexelDecodingFunction( 'envMapTexelToLinear', parameters.envMapEncoding ) : '',
			parameters.emissiveMapEncoding ? getTexelDecodingFunction( 'emissiveMapTexelToLinear', parameters.emissiveMapEncoding ) : '',
			parameters.outputEncoding ? getTexelEncodingFunction( 'linearToOutputTexel', parameters.outputEncoding ) : '',

			parameters.depthPacking ? '#define DEPTH_PACKING ' + material.depthPacking : '',

			'\n'

		].filter( filterEmptyLine ).join( '\n' );

	}
// parse:解析   解析Includes：            #include <common>
	vertexShader = parseIncludes( vertexShader );
	vertexShader = replaceLightNums( vertexShader, parameters );
	vertexShader = replaceClippingPlaneNums( vertexShader, parameters );

	fragmentShader = parseIncludes( fragmentShader );
	fragmentShader = replaceLightNums( fragmentShader, parameters );
	fragmentShader = replaceClippingPlaneNums( fragmentShader, parameters );

	vertexShader = unrollLoops( vertexShader );
	fragmentShader = unrollLoops( fragmentShader );
// 顶点、片元着色器代码的字符串经过一系列的处理
// 比如每一种灯光数量的替换   原来着色器中灯光的数量用了一个占位符代替  编译之前要先进行替换
	var vertexGlsl = prefixVertex + vertexShader;
	var fragmentGlsl = prefixFragment + fragmentShader;

	// console.log( '*VERTEX*', vertexGlsl );
	// console.log( '*FRAGMENT*', fragmentGlsl );
// WebGLShader编译着色器代码    返回shader对象
	var glVertexShader = WebGLShader( gl, gl.VERTEX_SHADER, vertexGlsl );
	var glFragmentShader = WebGLShader( gl, gl.FRAGMENT_SHADER, fragmentGlsl );
// 绑定程序对象和着色器对象
	gl.attachShader( program, glVertexShader );
	gl.attachShader( program, glFragmentShader );

	// Force a particular attribute to index 0.

	if ( material.index0AttributeName !== undefined ) {

		gl.bindAttribLocation( program, 0, material.index0AttributeName );

	} else if ( parameters.morphTargets === true ) {

		// programs with morphTargets displace position out of attribute 0
		gl.bindAttribLocation( program, 0, 'position' );

	}

	gl.linkProgram( program );

	var programLog = gl.getProgramInfoLog( program ).trim();
	var vertexLog = gl.getShaderInfoLog( glVertexShader ).trim();
	var fragmentLog = gl.getShaderInfoLog( glFragmentShader ).trim();

	var runnable = true;
	var haveDiagnostics = true;

	// console.log( '**VERTEX**', gl.getExtension( 'WEBGL_debug_shaders' ).getTranslatedShaderSource( glVertexShader ) );
	// console.log( '**FRAGMENT**', gl.getExtension( 'WEBGL_debug_shaders' ).getTranslatedShaderSource( glFragmentShader ) );

	if ( gl.getProgramParameter( program, gl.LINK_STATUS ) === false ) {

		runnable = false;

		console.error( 'THREE.WebGLProgram: shader error: ', gl.getError(), 'gl.VALIDATE_STATUS', gl.getProgramParameter( program, gl.VALIDATE_STATUS ), 'gl.getProgramInfoLog', programLog, vertexLog, fragmentLog );

	} else if ( programLog !== '' ) {

		console.warn( 'THREE.WebGLProgram: gl.getProgramInfoLog()', programLog );

	} else if ( vertexLog === '' || fragmentLog === '' ) {

		haveDiagnostics = false;

	}

	if ( haveDiagnostics ) {

		this.diagnostics = {

			runnable: runnable,
			material: material,

			programLog: programLog,

			vertexShader: {

				log: vertexLog,
				prefix: prefixVertex

			},

			fragmentShader: {

				log: fragmentLog,
				prefix: prefixFragment

			}

		};

	}

	// clean up

	gl.deleteShader( glVertexShader );
	gl.deleteShader( glFragmentShader );

	// set up caching for uniform locations

	var cachedUniforms;

	this.getUniforms = function () {

		if ( cachedUniforms === undefined ) {

			cachedUniforms = new WebGLUniforms( gl, program, renderer );

		}

		return cachedUniforms;

	};

	// set up caching for attribute locations

	var cachedAttributes;

	this.getAttributes = function () {

		if ( cachedAttributes === undefined ) {

			cachedAttributes = fetchAttributeLocations( gl, program );

		}

		return cachedAttributes;

	};

	// free resource

	this.destroy = function () {

		gl.deleteProgram( program );
		this.program = undefined;

	};

	// DEPRECATED

	Object.defineProperties( this, {

		uniforms: {
			get: function () {

				console.warn( 'THREE.WebGLProgram: .uniforms is now .getUniforms().' );
				return this.getUniforms();

			}
		},

		attributes: {
			get: function () {

				console.warn( 'THREE.WebGLProgram: .attributes is now .getAttributes().' );
				return this.getAttributes();

			}
		}

	} );


	//

	this.name = shader.name;
	this.id = programIdCount ++;
	this.code = code;
	this.usedTimes = 1;
	this.program = program;
	this.vertexShader = glVertexShader;
	this.fragmentShader = glFragmentShader;

	return this;

}

export { WebGLProgram };
