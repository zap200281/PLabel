///增加

var VIA_VERSION      = '1.0.0';
var VIA_NAME         = 'Image Annotator';
var VIA_SHORT_NAME   = 'pcl';
var VIA_REGION_SHAPE = { RECT:'rect',
                         CIRCLE:'circle',
                         ELLIPSE:'ellipse',
                         POLYGON:'polygon',
                         POINT:'point',
                         POLYLINE:'polyline'
                       };


var VIA_ATTRIBUTE_TYPE = { TEXT:'text',
                           CHECKBOX:'checkbox',
                           RADIO:'radio',
                           DROPDOWN:'dropdown'
                         };                        

var VIA_DISPLAY_AREA_CONTENT_NAME = {IMAGE:'image_panel',
                                     IMAGE_GRID:'image_grid_panel',
                                     SETTINGS:'settings_panel',
                                     PAGE_404:'page_404',
                                     PAGE_GETTING_STARTED:'page_getting_started',
                                     PAGE_ABOUT:'page_about',
                                     PAGE_START_INFO:'page_start_info',
                                     PAGE_LICENSE:'page_license'
                                    };

var VIA_ANNOTATION_EDITOR_MODE    = {SINGLE_REGION:'single_region',
                                     ALL_REGIONS:'all_regions'};
var VIA_ANNOTATION_EDITOR_PLACEMENT = {NEAR_REGION:'NEAR_REGION',
                                       IMAGE_BOTTOM:'IMAGE_BOTTOM',
                                       DISABLE:'DISABLE'};

// var VIA_POLYGON_RESIZE_VERTEX_OFFSET  = 100;
var VIA_CANVAS_DEFAULT_ZOOM_LEVEL_INDEX = 3;

var VIA_THEME_SEL_REGION_OPACITY    = 0.5;
var VIA_THEME_MESSAGE_TIMEOUT_MS    = 6000;
// var VIA_THEME_CONTROL_POINT_COLOR   = '#ff0000';

var VIA_CSV_SEP        = ',';
var VIA_CSV_QUOTE_CHAR = '"';
var VIA_CSV_KEYVAL_SEP = ':';

var _via_img_metadata = {};   // data structure to store loaded images metadata
// var regions=new Array(500);

var _via_img_src      = {};   // image content {abs. path, url, base64 data, etc}
var _via_img_fileref  = {};   // reference to local images selected by using browser file selector
var _via_img_count    = 0;    // count of the loaded images
var _via_canvas_regions = []; // image regions spec. in canvas space
var _via_canvas_scale   = 1.0;// current scale of canvas image

var _via_image_id       = ''; // id={filename+length} of current image
var _via_image_index    = -1; // index

var _via_current_image_filename;
var _via_current_image;
var _via_current_image_width;
var _via_current_image_height;

// a record of image statistics (e.g. width, height)
var _via_img_stat     = {};
var _via_is_all_img_stat_read_ongoing = false;
var _via_img_stat_current_img_index = false;

// image canvas
// var _via_display_area = document.getElementById('labelwin');
var _via_display_area = document.getElementById('show_region'); 
var _via_img_panel    = document.getElementById('image_panel');
// var _via_reg_canvas   = document.getElementById('region_canvas');
var _via_reg_ctx; // initialized in _via_init()
var _via_canvas_width, _via_canvas_height;

// canvas zoom
var _via_canvas_zoom_level_index   = VIA_CANVAS_DEFAULT_ZOOM_LEVEL_INDEX; // 1.0
var _via_canvas_scale_without_zoom = 1.0;

// state of the application
var _via_is_user_drawing_region  = false;
var _via_current_image_loaded    = false;
// var _via_current_image_loaded    = true;
var _via_is_window_resized       = false;
var _via_is_window_resized       = false;
var _via_is_user_resizing_region = false;
var _via_is_user_moving_region   = false;
var _via_is_user_drawing_polygon = false;
var _via_is_region_selected      = false;
var _via_is_all_region_selected  = false;
var _via_is_loaded_img_list_visible  = false;
var _via_is_attributes_panel_visible = false;
var _via_is_reg_attr_panel_visible   = false;
var _via_is_file_attr_panel_visible  = false;
var _via_is_canvas_zoomed            = false;
var _via_is_loading_current_image    = false;
var _via_is_region_id_visible        = true;
var _via_is_region_boundary_visible  = true;
var _via_is_region_info_visible      = false;
var _via_is_ctrl_pressed             = false;
var _via_is_debug_mode               = false;

// region
var _via_current_shape             = VIA_REGION_SHAPE.RECT;
var _via_current_polygon_region_id = -1;
var _via_user_sel_region_id        = -1;
var _via_click_x0 = 0; var _via_click_y0 = 0;
var _via_click_x1 = 0; var _via_click_y1 = 0;
var _via_region_click_x, _via_region_click_y;
var _via_region_edge          = [-1, -1];
var _via_current_x = 0; var _via_current_y = 0;

// region copy/paste
var _via_region_selected_flag = []; // region select flag for current image
var _via_copied_image_regions = [];
var _via_paste_to_multiple_images_input;

// message
var _via_message_clear_timer;

// attributes
var _via_attribute_being_updated       = 'region'; // {region, file}
// var _via_attributes                    = { 'region':{}, 'file':{} };
var _via_attributes                    = { 'region':{}};
var _via_current_attribute_id          = '';

// region group color
var _via_canvas_regions_group_color = {}; // color of each region

// invoke a method after receiving user input
var _via_user_input_ok_handler     = null;
var _via_user_input_cancel_handler = null;
var _via_user_input_data           = {};

// annotation editor
var _via_annotaion_editor_panel     = document.getElementById('annotation_editor_panel');
var _via_metadata_being_updated     = 'region'; // {region, file}
var _via_annotation_editor_mode     = VIA_ANNOTATION_EDITOR_MODE.SINGLE_REGION;

// persistence to local storage
var _via_is_local_storage_available = false;
var _via_is_save_ongoing            = false;


// the contents of _via_img_fn_list_img_index_list.
var _via_image_id_list                 = []; // array of all image id (in order they were added by user)
var _via_image_filename_list           = []; // array of all image filename
var _via_image_load_error              = []; // {true, false}
var _via_image_filepath_resolved       = []; // {true, false}
var _via_image_filepath_id_list        = []; // path for each file

var _via_reload_img_fn_list_table      = true;
var _via_img_fn_list_img_index_list    = []; // image index list of images show in img_fn_list
var _via_img_fn_list_html              = []; // html representation of image filename list

// image grid
var image_grid_panel                        = document.getElementById('image_grid_panel');
var _via_display_area_content_name          = ''; // describes what is currently shown in display area
var _via_display_area_content_name_prev     = '';
var _via_image_grid_requires_update         = false;
var _via_image_grid_content_overflow        = false;
var _via_image_grid_load_ongoing            = false;
var _via_image_grid_page_first_index        = 0; // array index in _via_img_fn_list_img_index_list[]
var _via_image_grid_page_last_index         = -1;
var _via_image_grid_selected_img_index_list = [];
var _via_image_grid_page_img_index_list     = []; // list of all image index in current page of image grid
var _via_image_grid_visible_img_index_list  = []; // list of images currently visible in grid
var _via_image_grid_mousedown_img_index     = -1;
var _via_image_grid_mouseup_img_index       = -1;
var _via_image_grid_img_index_list          = []; // list of all image index in the image grid
var _via_image_grid_region_index_list       = []; // list of all image index in the image grid
var _via_image_grid_group                   = {}; // {'value':[image_index_list]}
var _via_image_grid_group_var               = []; // {type, name, value}
var _via_image_grid_group_show_all          = false;
var _via_image_grid_stack_prev_page         = []; // stack of first img index of every page navigated so far

// image buffer
var VIA_IMG_PRELOAD_INDICES         = [1, -1, 2, 3, -2, 4]; // for any image, preload previous 2 and next 4 images
var VIA_IMG_PRELOAD_COUNT           = 4;
var _via_buffer_preload_img_index   = -1;
var _via_buffer_img_index_list      = [];
var _via_buffer_img_shown_timestamp = [];
var _via_preload_img_promise_list   = [];

// via settings
var _via_settings = {};
_via_settings.ui  = {};
_via_settings.ui.annotation_editor_height   = 25; // in percent of the height of browser window
_via_settings.ui.annotation_editor_fontsize = 0.8;// in rem
//_via_settings.ui.leftsidebar_width          = 18;  // in rem
_via_settings.ui.leftsidebar_width       = 43 

_via_settings.ui.image_grid = {};
_via_settings.ui.image_grid.img_height          = 80;  // in pixel
_via_settings.ui.image_grid.rshape_fill         = 'none';
_via_settings.ui.image_grid.rshape_fill_opacity = 0.3;
_via_settings.ui.image_grid.rshape_stroke       = 'yellow';
_via_settings.ui.image_grid.rshape_stroke_width = 2;
_via_settings.ui.image_grid.show_region_shape   = true;
_via_settings.ui.image_grid.show_image_policy   = 'all';

_via_settings.ui.image = {};
_via_settings.ui.image.region_label      = '__via_region_id__'; // default: region_id
_via_settings.ui.image.region_color      = '__via_default_region_color__'; // default color: yellow
_via_settings.ui.image.region_label_font = '10px Sans';
_via_settings.ui.image.on_image_annotation_editor_placement = VIA_ANNOTATION_EDITOR_PLACEMENT.NEAR_REGION;

_via_settings.core                  = {};
_via_settings.core.buffer_size      = 4*VIA_IMG_PRELOAD_COUNT + 2;
_via_settings.core.filepath         = {};
_via_settings.core.default_filepath = '';

// UI html elements
var invisible_file_input = document.getElementById("invisible_file_input");
// var display_area    = document.getElementById("display_area");
 var display_area    = document.getElementById("show_region");
var ui_top_panel    = document.getElementById("ui_top_panel");
var image_panel     = document.getElementById("image_panel");
var img_buffer_now  = document.getElementById("img_buffer_now");

var annotation_list_snippet = document.getElementById("annotation_list_snippet");
var annotation_textarea     = document.getElementById("annotation_textarea");

var img_fn_list_panel     = document.getElementById('img_fn_list_panel');
var img_fn_list           = document.getElementById('img_fn_list');
var attributes_panel      = document.getElementById('attributes_panel');
var leftsidebar           = document.getElementById('leftsidebar');

var BBOX_LINE_WIDTH       = 4;
var BBOX_SELECTED_OPACITY = 0.3;
var BBOX_BOUNDARY_FILL_COLOR_ANNOTATED = "#f2f2f2";
var BBOX_BOUNDARY_FILL_COLOR_NEW       = "#aaeeff";
var BBOX_BOUNDARY_LINE_COLOR           = "#1a1a1a";
var BBOX_SELECTED_FILL_COLOR           = "#ffffff";

var VIA_ANNOTATION_EDITOR_HEIGHT_CHANGE   = 5;   // in percent
var VIA_ANNOTATION_EDITOR_FONTSIZE_CHANGE = 0.1; // in rem
var VIA_IMAGE_GRID_IMG_HEIGHT_CHANGE      = 20;  // in percent
var VIA_LEFTSIDEBAR_WIDTH_CHANGE          = 1;   // in rem
var VIA_POLYGON_SEGMENT_SUBTENDED_ANGLE   = 5;   // in degree (used to approximate shapes using polygon)
var VIA_FLOAT_PRECISION = 3; // number of decimal places to include in float values



function file_region() {
  this.shape_attributes  = {}; // region shape attributes
  this.region_attributes = {}; // region attributes
}

//
// Initialization routine
//
function _via_init() {
  console.log(VIA_NAME);
  show_message(VIA_NAME + ' (' + VIA_SHORT_NAME + ') version ' + VIA_VERSION +
               '. Ready !', 2*VIA_THEME_MESSAGE_TIMEOUT_MS);

  if ( _via_is_debug_mode ) {
    document.getElementById('ui_top_panel').innerHTML += '<span>DEBUG MODE</span>';
  }

  // document.getElementById('img_fn_list').style.display = 'block';
  document.getElementById('leftsidebar').style.display = 'table-cell';


  init_leftsidebar_accordion();

  init_message_panel();

  // run attached sub-modules (if any)
  // e.g. demo modules
  if (typeof _via_load_submodules === 'function') {
    console.log('Loading VIA submodule');
    setTimeout( async function() {
      await _via_load_submodules();
    }, 100);
  }

}



function is_content_name_valid(content_name) {
  var e;
  for ( e in VIA_DISPLAY_AREA_CONTENT_NAME ) {
    if ( VIA_DISPLAY_AREA_CONTENT_NAME[e] === content_name ) {
      return true;
    }
  }
  return false;
}



function set_display_area_content(content_name) {
  if ( is_content_name_valid(content_name) ) {
    _via_display_area_content_name_prev = _via_display_area_content_name;
    _via_display_area_content_name = content_name;
  }
}


//
// Maintainers of user interface
//

function init_message_panel() {
  var p = document.getElementById('message_panel');
  p.addEventListener('mousedown', function() {
    this.style.display = 'none';
  }, false);
  p.addEventListener('mouseover', function() {
    clearTimeout(_via_message_clear_timer); // stop any previous timeouts
  }, false);
}

function show_message(msg, t) {
  if ( _via_message_clear_timer ) {
    clearTimeout(_via_message_clear_timer); // stop any previous timeouts
  }
  var timeout = t;
  if ( typeof t === 'undefined' ) {
    timeout = VIA_THEME_MESSAGE_TIMEOUT_MS;
  }
  document.getElementById('message_panel_content').innerHTML = msg;
  document.getElementById('message_panel').style.display = 'block';

  _via_message_clear_timer = setTimeout( function() {
    document.getElementById('message_panel').style.display = 'none';
  }, timeout);
}

function _via_regions_group_color_init() {
  _via_canvas_regions_group_color = {};
  var aid = _via_settings.ui.image.region_color;
  if ( aid !== '__via_default_region_color__' ) {
    var avalue;
    for ( var i = 0; i < regions.length; ++i ) {
      avalue = regions[i].other.region_attributes[aid];
      _via_canvas_regions_group_color[avalue] = 1;
    }
    var color_index = 0;
    for ( avalue in _via_canvas_regions_group_color ) {
      _via_canvas_regions_group_color[avalue] = VIA_REGION_COLOR_LIST[ color_index % VIA_REGION_COLOR_LIST.length ];
      color_index = color_index + 1;
    }
  }
}



function toggle_all_regions_selection(is_selected) {
  var n = regions.length;
  var i;
  _via_region_selected_flag = [];
  for ( i = 0; i < n; ++i) {
    _via_region_selected_flag[i] = is_selected;
  }
  _via_is_all_region_selected = is_selected;
  annotation_editor_hide();
  if ( _via_annotation_editor_mode === VIA_ANNOTATION_EDITOR_MODE.ALL_REGIONS ) {
    annotation_editor_clear_row_highlight();
  }
}

function select_only_region(region_id) {
  // toggle_all_regions_selection(false);
  // set_region_select_state(region_id, true);
  _via_is_region_selected = true;
  _via_is_all_region_selected = false;
  _via_user_sel_region_id = region_id;
}

function set_region_select_state(region_id, is_selected) {
  _via_region_selected_flag[region_id] = is_selected;
}





// source: https://www.w3schools.com/howto/howto_js_accordion.asp
function init_leftsidebar_accordion() {
  var leftsidebar = document.getElementById('leftsidebar');
  // leftsidebar.style.width = _via_settings.ui.leftsidebar_width + 'rem';

  var acc = document.getElementsByClassName('leftsidebar_accordion');
  var i;
  for ( i = 0; i < acc.length; ++i ) {
    acc[i].addEventListener('click', function() {
      update_vertical_space();
      this.classList.toggle('active');
      this.nextElementSibling.classList.toggle('show');

      switch( this.innerHTML ) {
      case 'Attributes':
        update_attributes_update_panel();
        break;
      // case 'Project':
      //   update_img_fn_list();
      //   break;
      }
    });
  }
}




// this vertical spacer is needed to allow scrollbar to show
// items like Keyboard Shortcut hidden under the attributes panel
function update_vertical_space() {
  var panel = document.getElementById('vertical_space');
  var aepanel = document.getElementById('annotation_editor_panel');
  // panel.style.height = (aepanel.offsetHeight + 280) + 'px';
}

//
// region and file attributes update panel
//
function attribute_update_panel_set_active_button() {
  var attribute_type;
  for ( attribute_type in _via_attributes ) {
    var bid = 'button_show_' + attribute_type + '_attributes';
    document.getElementById(bid).classList.remove('active');
  }
  var bid = 'button_show_' + _via_attribute_being_updated + '_attributes';
  document.getElementById(bid).classList.add('active');
}

function show_region_attributes_update_panel() {
  _via_attribute_being_updated = 'region';
  var rattr_list = Object.keys(_via_attributes['region']);
  if ( rattr_list.length ) {
    _via_current_attribute_id = rattr_list[0];
  } else {
    _via_current_attribute_id = '';
  }
  update_attributes_update_panel();
  attribute_update_panel_set_active_button();

}




function update_attributes_name_list() {
  var p = document.getElementById('attributes_name_list');
  p.innerHTML = '';

  var attr;


  var html=" <tr>\
            <th>属性值</th>\
            <th>操作</th>\
            </tr> ";

  for ( attr in _via_attributes[_via_attribute_being_updated] ){
      var row = "<tr>\
                  <td id =\"attr\">"+attr+ "</td>"+
                  "<td>"+
                  "<a onclick=\"sessionStorage.setItem(\'attr_id\',\'"+attr+"\'); show_attribute_properties(); show_attribute_options()\" class=\"btn btn-xs btn-success\">" +"显示属性"+ "</a>"  + "&nbsp;&nbsp;&nbsp;<a onclick=\"sessionStorage.setItem(\'attr_id\',\'"+attr+"\');delete_existing_attribute_with_confirm()\"; class=\"btn btn-xs btn-success\">删除属性</a>" +
                  // "<td>" + "<a onclick= \"sessionStorage.setItem(\'attr_id\',\'"+attr+"\'); show_attribute_properties(); show_attribute_options();\">" +attr+ "</a>" +
                  "</td>"+
                "</tr>";

      html=html+row;
  }
  document.getElementById('attributes_name_list').innerHTML=html;

}

function update_attributes_update_panel() {
  if ( document.getElementById('attributes_editor_panel').classList.contains('show') ) {
    update_attributes_name_list();
    // show_attribute_properties();
    // show_attribute_options();
  }
}

function update_attribute_properties_panel() {
  if ( document.getElementById('attributes_editor_panel').classList.contains('show') ) {
    show_attribute_properties();
    show_attribute_options();
  }
}

function show_attribute_properties() {
  // var attr_list = document.getElementById('attributes_name_list');
  document.getElementById('attribute_properties').innerHTML = '';

  var attr_id = sessionStorage.getItem("attr_id");
  var attr_type = _via_attribute_being_updated;
  // if (attr_id == "color"){
  //    _via_attributes[attr_type][attr_id].type = "dropdown";
  // }
  var attr_input_type = _via_attributes[attr_type][attr_id].type;

  var attr_desc = _via_attributes[attr_type][attr_id].description;

  attribute_property_add_input_property('Name of attribute (appears in exported annotations)',
                                        'Name',
                                        attr_id,
                                        'attribute_name');
  attribute_property_add_input_property('Description of attribute (shown to user during annotation session)',
                                        'Desc.',
                                        attr_desc,
                                        'attribute_description');

  if ( attr_input_type === 'text' ) {
    var attr_default_value = _via_attributes[attr_type][attr_id].default_value;
    attribute_property_add_input_property('Default value of this attribute',
                                          'Def.',
                                          attr_default_value,
                                          'attribute_default_value');
  }

  // add dropdown for type of attribute
  var p = document.createElement('div');
  p.setAttribute('class', 'property');
  var c0 = document.createElement('span');
  c0.setAttribute('title', 'Attribute type (e.g. text, checkbox, radio, etc)');
  c0.innerHTML = 'Type';
  var c1 = document.createElement('span');
  var c1b = document.createElement('select');
  c1b.setAttribute('onchange', 'attribute_property_on_update(this)');
  c1b.setAttribute('id', 'attribute_type');
  var type_id;
  for ( type_id in VIA_ATTRIBUTE_TYPE ) {
    var type = VIA_ATTRIBUTE_TYPE[type_id];
    var option = document.createElement('option');
    option.setAttribute('value', type);
    option.innerHTML = type;
    if ( attr_input_type == type ) {
      option.setAttribute('selected', 'selected');
    }
    c1b.appendChild(option);
  }
  c1.appendChild(c1b);
  p.appendChild(c0);
  p.appendChild(c1);
  document.getElementById('attribute_properties').appendChild(p);
}

function show_attribute_options() {
  // var attr_list = document.getElementById('attributes_name_list');
  document.getElementById('attribute_options').innerHTML = '';
   document.getElementById('attribute_options').innerHTML = '';


  // var attr_id = attr_list.value;
  var attr_id = sessionStorage.getItem("attr_id");
  var attr_type = _via_attributes[_via_attribute_being_updated][attr_id].type;

  // populate additional options based on attribute type
  switch( attr_type ) {
  case VIA_ATTRIBUTE_TYPE.TEXT:
    // text does not have any additional properties
    break;
  case VIA_ATTRIBUTE_TYPE.IMAGE:
    var p = document.createElement('div');
    p.setAttribute('class', 'property');
    p.setAttribute('style', 'text-align:center');
    var c0 = document.createElement('span');
    c0.setAttribute('style', 'width:25%');
    c0.setAttribute('title', 'When selected, this is the value that appears in exported annotations');
    c0.innerHTML = 'id';
    var c1 = document.createElement('span');
    c1.setAttribute('style', 'width:60%');
    c1.setAttribute('title', 'URL or base64 (see https://www.base64-image.de/) encoded image data that corresponds to the image shown as an option to the annotator');
    c1.innerHTML = 'image url or b64';
    var c2 = document.createElement('span');
    c2.setAttribute('title', 'The default value of this attribute');
    c2.innerHTML = 'def.';
    p.appendChild(c0);
    p.appendChild(c1);
    p.appendChild(c2);
    document.getElementById('attribute_options').appendChild(p);

    var options = _via_attributes[_via_attribute_being_updated][attr_id].options;
    var option_id;
    for ( option_id in options ) {
      var option_desc = options[option_id];

      var option_default = _via_attributes[_via_attribute_being_updated][attr_id].default_options[option_id];
      attribute_property_add_option(attr_id, option_id, option_desc, option_default, attr_type);
    }
    attribute_property_add_new_entry_option(attr_id, attr_type);
    break;
  case VIA_ATTRIBUTE_TYPE.CHECKBOX: // handled by next case
  case VIA_ATTRIBUTE_TYPE.DROPDOWN: // handled by next case
  case VIA_ATTRIBUTE_TYPE.RADIO:
    var p = document.createElement('div');
    p.setAttribute('class', 'property');
    p.setAttribute('style', 'text-align:center');
    var c0 = document.createElement('span');
    c0.setAttribute('style', 'width:25%');
    c0.setAttribute('title', 'When selected, this is the value that appears in exported annotations');
    c0.innerHTML = 'id';
    var c1 = document.createElement('span');
    c1.setAttribute('style', 'width:60%');
    c1.setAttribute('title', 'This is the text shown as an option to the annotator');
    c1.innerHTML = 'description';
    var c2 = document.createElement('span');
    c2.setAttribute('title', 'The default value of this attribute');
    c2.innerHTML = 'def.';
    p.appendChild(c0);
    p.appendChild(c1);
    p.appendChild(c2);
    document.getElementById('attribute_options').appendChild(p);

    var options = _via_attributes[_via_attribute_being_updated][attr_id].options;
    var option_id;
    for ( option_id in options ) {
      var option_desc = options[option_id];

      var option_default = _via_attributes[_via_attribute_being_updated][attr_id].default_options[option_id];
      attribute_property_add_option(attr_id, option_id, option_desc, option_default, attr_type);
    }
    attribute_property_add_new_entry_option(attr_id, attr_type);
    break;
  default:
    console.log('Attribute type ' + attr_type + ' is unavailable');
  }
}

function attribute_property_add_input_property(title, name, value, id) {
  var p = document.createElement('div');
  p.setAttribute('class', 'property');
  var c0 = document.createElement('span');
  c0.setAttribute('title', title);
  c0.innerHTML = name;
  var c1 = document.createElement('span');
  var c1b = document.createElement('input');
  c1b.setAttribute('onchange', 'attribute_property_on_update(this)');
  if ( typeof(value) !== 'undefined' ) {
    c1b.setAttribute('value', value);
  }
  c1b.setAttribute('id', id);
  c1.appendChild(c1b);
  p.appendChild(c0);
  p.appendChild(c1);

  document.getElementById('attribute_properties').appendChild(p);
}

function attribute_property_add_option(attr_id, option_id, option_desc, option_default, attribute_type) {
  var p = document.createElement('div');
  p.setAttribute('class', 'property');
  var c0 = document.createElement('span');
  var c0b = document.createElement('input');
  c0b.setAttribute('type', 'text');
  c0b.setAttribute('value', option_id);
  c0b.setAttribute('title', option_id);
  c0b.setAttribute('onchange', 'attribute_property_on_option_update(this)');
  c0b.setAttribute('id', '_via_attribute_option_id_' + option_id);

  var c1 = document.createElement('span');
  var c1b = document.createElement('input');
  c1b.setAttribute('type', 'text');

  if ( attribute_type === VIA_ATTRIBUTE_TYPE.IMAGE ) {
    var option_desc_info = option_desc.length + ' bytes of base64 image data';
    c1b.setAttribute('value', option_desc_info);
    c1b.setAttribute('title', 'To update, copy and paste base64 image data in this text box');
  } else {
    c1b.setAttribute('value', option_desc);
    c1b.setAttribute('title', option_desc);
  }
  c1b.setAttribute('onchange', 'attribute_property_on_option_update(this)');
  c1b.setAttribute('id', '_via_attribute_option_description_' + option_id);

  var c2 = document.createElement('span');
  var c2b = document.createElement('input');
  c2b.setAttribute('type', attribute_type);
  if ( typeof option_default !== 'undefined' ) {
    c2b.checked = option_default;
  }
  if ( attribute_type === 'radio' || attribute_type === 'image' || attribute_type === 'dropdown' ) {
    // ensured that user can activate only one radio button
    c2b.setAttribute('type', 'radio');
    c2b.setAttribute('name', attr_id);
  }

  c2b.setAttribute('onchange', 'attribute_property_on_option_update(this)');
  c2b.setAttribute('id', '_via_attribute_option_default_' + option_id);

  c0.appendChild(c0b);
  c1.appendChild(c1b);
  c2.appendChild(c2b);
  p.appendChild(c0);
  p.appendChild(c1);
  p.appendChild(c2);

  document.getElementById('attribute_options').appendChild(p);
}

function attribute_property_add_new_entry_option(attr_id, attribute_type) {
  var p = document.createElement('div');
  p.setAttribute('class', 'new_option_id_entry');
  var c0b = document.createElement('input');
  c0b.setAttribute('type', 'text');
  c0b.setAttribute('onchange', 'attribute_property_on_option_add(this)');
  c0b.setAttribute('id', '_via_attribute_new_option_id');
  c0b.setAttribute('placeholder', 'Add new option id');
  p.appendChild(c0b);
  document.getElementById('attribute_options').appendChild(p);
}

function attribute_property_on_update(p) {
  var attr_id = get_current_attribute_id();
  var attr_type = _via_attribute_being_updated;
  var attr_value = p.value;

  switch(p.id) {
  case 'attribute_name':
    if ( attr_value !== attr_id ) {
      Object.defineProperty(_via_attributes[attr_type],
                            attr_value,
                            Object.getOwnPropertyDescriptor(_via_attributes[attr_type], attr_id));

      delete _via_attributes[attr_type][attr_id];
      update_attributes_update_panel();
      annotation_editor_update_content(regions);
    }
    break;
  case 'attribute_description':
    _via_attributes[attr_type][attr_id].description = attr_value;
    update_attributes_update_panel();
    annotation_editor_update_content(regions);
    break;
  case 'attribute_default_value':
    _via_attributes[attr_type][attr_id].default_value = attr_value;
    update_attributes_update_panel();
    annotation_editor_update_content(regions);
    break;
  case 'attribute_type':
    _via_attributes[attr_type][attr_id].type = attr_value;
    if( attr_value === VIA_ATTRIBUTE_TYPE.TEXT ) {
      _via_attributes[attr_type][attr_id].default_value = '';
      delete _via_attributes[attr_type][attr_id].options;
      delete _via_attributes[attr_type][attr_id].default_options;
    } else {
      // preserve existing options
      if ( ! _via_attributes[attr_type][attr_id].hasOwnProperty('options') ) {
        _via_attributes[attr_type][attr_id].options = {};
        _via_attributes[attr_type][attr_id].default_options = {};
      }

      if ( _via_attributes[attr_type][attr_id].hasOwnProperty('default_value') ) {
        delete _via_attributes[attr_type][attr_id].default_value;
      }

      // collect existing attribute values and add them as options
      var attr_values = attribute_get_unique_values(attr_type, attr_id);
      var i;
      for ( i = 0; i < attr_values.length; ++i ) {
        var attr_val = attr_values[i];
        if ( attr_val !== '' ) {
          _via_attributes[attr_type][attr_id].options[attr_val] = attr_val;
        }
      }
    }
    show_attribute_properties();
    show_attribute_options();
    annotation_editor_update_content(regions);
    break;
  }
}

function attribute_get_unique_values(attr_type, attr_id) {
  var values = [];
  switch ( attr_type ) {

  case 'region':
    var img_id, attr_val, i;
    // for ( img_id in _via_img_metadata ) {
     if (regions!== undefined){
      for ( i = 0; i < regions.length; ++i ) {
        // if (_via_img_metadata[img_id].regions[i].hasOwnProperty("region_attributes")){
        if (regions[i]!== undefined){
            if ( regions[i].other.region_attributes.hasOwnProperty(attr_id) ) {
              attr_val = regions[i].other.region_attributes[attr_id];
              if ( ! values.includes(attr_val) ) {
                values.push(attr_val);
              }
            }
        }


      }
        }
    // }
    break;
  default:
    break;
  }
  return values;
}

function attribute_property_on_option_update(p) {
  var attr_id = get_current_attribute_id();
  if ( p.id.startsWith('_via_attribute_option_id_') ) {
    var old_key = p.id.substr( '_via_attribute_option_id_'.length );
    var new_key = p.value;
    if ( old_key !== new_key ) {
      var option_id_test = attribute_property_option_id_is_valid(attr_id, new_key);
      if ( option_id_test.is_valid ) {
        update_attribute_option_id_with_confirm(_via_attribute_being_updated,
                                                attr_id,
                                                old_key,
                                                new_key);
      } else {
        p.value = old_key; // restore old value
        show_message( option_id_test.message );
        show_attribute_properties();
      }
      return;
    }
  }

  if ( p.id.startsWith('_via_attribute_option_description_') ) {
    var key = p.id.substr( '_via_attribute_option_description_'.length );
    var old_value = _via_attributes[_via_attribute_being_updated][attr_id].options[key];
    var new_value = p.value;
    if ( new_value !== old_value ) {
      _via_attributes[_via_attribute_being_updated][attr_id].options[key] = new_value;
      show_attribute_properties();
      annotation_editor_update_content(regions);
    }
  }

  if ( p.id.startsWith('_via_attribute_option_default_') ) {
    var new_default_option_id = p.id.substr( '_via_attribute_option_default_'.length );
    var old_default_option_id_list = Object.keys(_via_attributes[_via_attribute_being_updated][attr_id].default_options);

    if ( old_default_option_id_list.length === 0 ) {
      // default set for the first time
      _via_attributes[_via_attribute_being_updated][attr_id].default_options[new_default_option_id] = p.checked;
    } else {
      switch ( _via_attributes[_via_attribute_being_updated][attr_id].type ) {
      case 'image':    // fallback
      case 'dropdown': // fallback
      case 'radio':    // fallback
        // to ensure that only one radio button is selected at a time
        _via_attributes[_via_attribute_being_updated][attr_id].default_options = {};
        _via_attributes[_via_attribute_being_updated][attr_id].default_options[new_default_option_id] = p.checked;
        break;
      case 'checkbox':
        _via_attributes[_via_attribute_being_updated][attr_id].default_options[new_default_option_id] = p.checked;
        break;
      }
    }
    // default option updated
    attribute_property_on_option_default_update(_via_attribute_being_updated,
                                                attr_id,
                                                new_default_option_id).then( function() {
                                                  show_attribute_properties();
                                                  annotation_editor_update_content(regons);
                                                });
  }
}

function attribute_property_on_option_default_update(attribute_being_updated, attr_id, new_default_option_id) {
  return new Promise( function(ok_callback, err_callback) {
    // set all metadata to new_value if:
    // - metadata[attr_id] is missing
    // - metadata[attr_id] is set to option_old_value
    var img_id, attr_value, n, i;
    var attr_type = _via_attributes[attribute_being_updated][attr_id].type;
    switch( attribute_being_updated ) {

    case 'region':
      // for ( img_id in _via_img_metadata ) {
        n = regions.length;
        for ( i = 0; i < n; ++i ) {
          if ( !regions[i].other.region_attributes.hasOwnProperty(attr_id) ) {
            regions[i].other.region_attributes[attr_id] = new_default_option_id;
          }
        }
      // }
      break;
    }
    ok_callback();
  });
}

function attribute_property_on_option_add(p) {
  if ( p.value === '' || p.value === null ) {
    return;
  }

  if ( p.id === '_via_attribute_new_option_id' ) {
    var attr_id = get_current_attribute_id();
    var option_id = p.value;
    var option_id_test = attribute_property_option_id_is_valid(attr_id, option_id);
    if ( option_id_test.is_valid ) {
      _via_attributes[_via_attribute_being_updated][attr_id].options[option_id] = '';
      show_attribute_options();
      annotation_editor_update_content(regions);
    } else {
      show_message( option_id_test.message );
      attribute_property_reset_new_entry_inputs();
    }
  }
}

function attribute_property_reset_new_entry_inputs() {
  var container = document.getElementById('attribute_options');
  var p = container.lastChild;
  console.log(p.childNodes)
  if ( p.childNodes[0] ) {
    p.childNodes[0].value = '';
  }
  if ( p.childNodes[1] ) {
    p.childNodes[1].value = '';
  }
}

function attribute_property_show_new_entry_inputs(attr_id, attribute_type) {
  var n0 = document.createElement('div');
  n0.classList.add('property');
  var n1a = document.createElement('span');
  var n1b = document.createElement('input');
  n1b.setAttribute('onchange', 'attribute_property_on_option_add(this)');
  n1b.setAttribute('placeholder', 'Add new id');
  n1b.setAttribute('value', '');
  n1b.setAttribute('id', '_via_attribute_new_option_id');
  n1a.appendChild(n1b);

  var n2a = document.createElement('span');
  var n2b = document.createElement('input');
  n2b.setAttribute('onchange', 'attribute_property_on_option_add(this)');
  n2b.setAttribute('placeholder', 'Optional description');
  n2b.setAttribute('value', '');
  n2b.setAttribute('id', '_via_attribute_new_option_description');
  n2a.appendChild(n2b);

  var n3a = document.createElement('span');
  var n3b = document.createElement('input');
  n3b.setAttribute('type', attribute_type);
  if ( attribute_type === 'radio' ) {
    n3b.setAttribute('name', attr_id);
  }
  n3b.setAttribute('onchange', 'attribute_property_on_option_add(this)');
  n3b.setAttribute('id', '_via_attribute_new_option_default');
  n3a.appendChild(n3b);

  n0.appendChild(n1a);
  n0.appendChild(n2a);
  n0.appendChild(n3a);

  var container = document.getElementById('attribute_options');
  container.appendChild(n0);
}

function attribute_property_option_id_is_valid(attr_id, new_option_id) {
  var option_id;
  for ( option_id in _via_attributes[_via_attribute_being_updated][attr_id].options ) {
    if ( option_id === new_option_id ) {
      return { 'is_valid':false, 'message':'Option id [' + attr_id + '] already exists' };
    }
  }

  if ( new_option_id.includes('__') ) { // reserved separator for attribute-id, row-id, option-id
    return {'is_valid':false, 'message':'Option id cannot contain two consecutive underscores'};
  }

  return {'is_valid':true};
}

function attribute_property_id_exists(name) {
  var attr_name;
  for ( attr_name in _via_attributes[_via_attribute_being_updated] ) {
    if ( attr_name === name ) {
      return true;
    }
  }
  return false;
}

function delete_existing_attribute_with_confirm() {
  // var attr_id = document.getElementById('user_input_attribute_id').value;
   var attr_id = sessionStorage.getItem("attr_id");
  if ( attr_id === '' ) {
    show_message('Enter the name of attribute that you wish to delete');
    return;
  }
  if ( attribute_property_id_exists(attr_id) ) {
    var config = {'title':'Delete ' + _via_attribute_being_updated + ' attribute [' + attr_id + ']' };
    var input = { 'attr_type':{'type':'text', 'name':'Attribute Type', 'value':_via_attribute_being_updated, 'disabled':true},
                  'attr_id':{'type':'text', 'name':'Attribute Id', 'value':attr_id, 'disabled':true}
                };
    if(attr_id === "type" || attr_id ==="id")
    {
        alert("不能删除属性type或者id,可以修改信息");
       // show_message(' does not delete type!');
        return;
    }
    delete_existing_attribute_confirmed(input);
    // invoke_with_user_inputs(delete_existing_attribute_confirmed, input, config);
    // document.getElementById('attributes_editor_panel').innerHTML = '';


  } else {
    show_message('Attribute [' + attr_id + '] does not exist!');
    return;
  }
}

function delete_existing_attribute_confirmed(input) {
  var attr_type = input.attr_type.value;
  var attr_id   = input.attr_id.value;
  delete_existing_attribute(attr_type, attr_id);
  document.getElementById('user_input_attribute_id').value = '';
  show_message('Deleted ' + attr_type + ' attribute [' + attr_id + ']');
  user_input_default_cancel_handler();

  document.getElementById('attribute_properties').innerHTML = '';
  document.getElementById('attribute_options').innerHTML = '';
}

function delete_existing_attribute(attribute_type, attribute_id) {
  if ( _via_attributes[attribute_type].hasOwnProperty( attribute_id ) ) {
    var attr_id_list = Object.keys(_via_attributes[attribute_type]);
    if ( attr_id_list.length === 1 ) {
      _via_current_attribute_id = '';
    } else {
      var current_index = attr_id_list.indexOf(attribute_id);
      var next_index = current_index + 1;
      if ( next_index === attr_id_list.length ) {
        next_index = current_index - 1;
      }
      _via_current_attribute_id = attr_id_list[next_index];
    }
    delete _via_attributes[attribute_type][attribute_id];
    update_attributes_update_panel();
    // annotation_editor_update_content();
  }
}

function add_new_attribute_from_user_input() {
  var attr_id = document.getElementById('user_input_attribute_id').value;
  if ( attr_id === '' ) {
    show_message('Enter the name of attribute that you wish to delete');
    return;
  }

  if ( attribute_property_id_exists(attr_id) ) {
    show_message('The ' + _via_attribute_being_updated + ' attribute [' + attr_id + '] already exists.');
  } else {
    _via_current_attribute_id = attr_id;
    add_new_attribute(attr_id);
    update_attributes_update_panel();
    annotation_editor_update_content(regions);
    show_message('Added ' + _via_attribute_being_updated + ' attribute [' + attr_id + '].');
  }
}

function add_new_attribute(attribute_id) {
  _via_attributes[_via_attribute_being_updated][attribute_id] = {};
  _via_attributes[_via_attribute_being_updated][attribute_id].type = 'text';
  _via_attributes[_via_attribute_being_updated][attribute_id].description = '';
  _via_attributes[_via_attribute_being_updated][attribute_id].default_value = '';
}

function update_current_attribute_id(p) {
  _via_current_attribute_id = p.options[p.selectedIndex].value;
  update_attribute_properties_panel();
}

function get_current_attribute_id() {
  // return document.getElementById('attributes_name_list').value;
   return sessionStorage.getItem("attr_id");
}

function update_attribute_option_id_with_confirm(attr_type, attr_id, option_id, new_option_id) {
  var is_delete = false;
  var config;
  if ( new_option_id === '' || typeof(new_option_id) === 'undefined' ) {
    // an empty new_option_id indicates deletion of option_id
    config = {'title':'Delete an option for ' + attr_type + ' attribute'};
    is_delete = true;
  } else {
    config = {'title':'Rename an option for ' + attr_type + ' attribute'};
  }

  var input = { 'attr_type':{'type':'text', 'name':'Attribute Type', 'value':attr_type, 'disabled':true},
                'attr_id':{'type':'text', 'name':'Attribute Id', 'value':attr_id, 'disabled':true}
              };

  if ( is_delete ) {
    input['option_id'] = {'type':'text', 'name':'Attribute Option', 'value':option_id, 'disabled':true};
  } else {
    input['option_id']     = {'type':'text', 'name':'Attribute Option (old)', 'value':option_id, 'disabled':true},
    input['new_option_id'] = {'type':'text', 'name':'Attribute Option (new)', 'value':new_option_id, 'disabled':true};
  }

  invoke_with_user_inputs(update_attribute_option_id_confirmed, input, config, update_attribute_option_id_cancel);
}

function update_attribute_option_id_cancel(input) {
  update_attribute_properties_panel();
}

function update_attribute_option_id_confirmed(input) {
  var attr_type = input.attr_type.value;
  var attr_id = input.attr_id.value;
  var option_id = input.option_id.value;
  var is_delete;
  var new_option_id;
  if ( typeof(input.new_option_id) === 'undefined' || input.new_option_id === '' ) {
    is_delete = true;
    new_option_id = '';
  } else {
    is_delete = false;
    new_option_id = input.new_option_id.value;
  }

  update_attribute_option(is_delete, attr_type, attr_id, option_id, new_option_id);

  if ( is_delete ) {
    show_message('Deleted option [' + option_id + '] for ' + attr_type + ' attribute [' + attr_id + '].');
  } else {
    show_message('Renamed option [' + option_id + '] to [' + new_option_id + '] for ' + attr_type + ' attribute [' + attr_id + '].');
  }
  update_attribute_properties_panel();
  annotation_editor_update_content();
  user_input_default_cancel_handler();
}

function update_attribute_option(is_delete, attr_type, attr_id, option_id, new_option_id) {
  switch ( attr_type ) {
  case 'region':
    update_region_attribute_option_in_all_metadata(is_delete, attr_id, option_id, new_option_id);
    if ( ! is_delete ) {
      Object.defineProperty(_via_attributes[attr_type][attr_id].options,
                            new_option_id,
                            Object.getOwnPropertyDescriptor(_via_attributes[_via_attribute_being_updated][attr_id].options, option_id));
    }
    delete _via_attributes['region'][attr_id].options[option_id];

    break;

  }
}



function update_region_attribute_option_in_all_metadata(is_delete, attr_id, option_id, new_option_id) {
  // var image_id;
  // for ( image_id in _via_img_metadata ) {
    update_region_attribute_option_from_metadata( is_delete, attr_id, option_id, new_option_id);
  // }
}

function update_region_attribute_option_from_metadata(is_delete, attr_id, option_id, new_option_id) {
  var i;
  if (regions!== undefined){
    for ( i = 0; i < regions.length; ++i ) {

        if ( regions[i]!==undefined){
          if ( regions[i].other.region_attributes.hasOwnProperty(attr_id) ) {
            if ( regions[i].other.region_attributes[attr_id].hasOwnProperty(option_id) ) {
              Object.defineProperty(regions[i].other.region_attributes[attr_id],
                                    new_option_id,
                                    Object.getOwnPropertyDescriptor(regions[i].other.region_attributes[attr_id], option_id));
              delete regions[i].other.region_attributes[attr_id][option_id];
            }
          }
        }
    }
  }
}



//
// invoke a method after receiving inputs from user
//
function invoke_with_user_inputs(ok_handler, input, config, cancel_handler) {
  setup_user_input_panel(ok_handler, input, config, cancel_handler);
  show_user_input_panel();
}

function setup_user_input_panel(ok_handler, input, config, cancel_handler) {

  _via_user_input_ok_handler = ok_handler;
  _via_user_input_cancel_handler = cancel_handler;
  _via_user_input_data = input;

  var p = document.getElementById('user_input_panel');
  var c = document.createElement('div');
  c.setAttribute('class', 'content');
  var html = [];
  html.push('<p class="title">' + config.title + '</p>');

  html.push('<div class="user_inputs">');
  var key;
  for ( key in _via_user_input_data ) {
    html.push('<div class="row">');
    html.push('<span class="cell">' + _via_user_input_data[key].name + '</span>');
    var disabled_html = '';
    if ( _via_user_input_data[key].disabled ) {
      disabled_html = 'disabled="disabled"';
    }
    var value_html = '';
    if ( _via_user_input_data[key].value ) {
      value_html = 'value="' + _via_user_input_data[key].value + '"';
    }

    switch(_via_user_input_data[key].type) {
    case 'checkbox':
      if ( _via_user_input_data[key].checked ) {
        value_html = 'checked="checked"';
      } else {
        value_html = '';
      }
      html.push('<span class="cell">' +
                '<input class="_via_user_input_variable" ' +
                value_html + ' ' +
                disabled_html + ' ' +
                'type="checkbox" id="' + key + '"></span>');
      break;
    case 'text':
      var size = '50';
      if ( _via_user_input_data[key].size ) {
        size = _via_user_input_data[key].size;
      }
      var placeholder = '';
      if ( _via_user_input_data[key].placeholder ) {
        placeholder = _via_user_input_data[key].placeholder;
      }
      html.push('<span class="cell">' +
                '<input class="_via_user_input_variable" ' +
                value_html + ' ' +
                disabled_html + ' ' +
                'size="' + size + '" ' +
                'placeholder="' + placeholder + '" ' +
                'type="text" id="' + key + '"></span>');

      break;
    case 'textarea':
      var rows = '2';
      var cols = '10'
      if ( _via_user_input_data[key].rows ) {
        rows = _via_user_input_data[key].rows;
      }
      if ( _via_user_input_data[key].cols ) {
        cols = _via_user_input_data[key].cols;
      }
      var placeholder = '';
      if ( _via_user_input_data[key].placeholder ) {
        placeholder = _via_user_input_data[key].placeholder;
      }
      html.push('<span class="cell">' +
                '<textarea class="_via_user_input_variable" ' +
                disabled_html + ' ' +
                'rows="' + rows + '" ' +
                'cols="' + cols + '" ' +
                'placeholder="' + placeholder + '" ' +
                'id="' + key + '">' + value_html + '</textarea></span>');

      break;

    }
    html.push('</div>'); // end of row
  }
  html.push('</div>'); // end of user_input div
  html.push('<div class="user_confirm">' +
            '<span class="ok">' +
            '<button id="user_input_ok_button" onclick="user_input_parse_and_invoke_handler()">&nbsp;OK&nbsp;</button></span>' +
            '<span class="cancel">' +
            '<button id="user_input_cancel_button" onclick="user_input_cancel_handler()">CANCEL</button></span></div>');
  c.innerHTML = html.join('');
  p.innerHTML = '';
  p.appendChild(c);

}

function user_input_default_cancel_handler() {
  hide_user_input_panel();
  _via_user_input_data = {};
  _via_user_input_ok_handler = null;
  _via_user_input_cancel_handler = null;
}

function user_input_cancel_handler() {
  if ( _via_user_input_cancel_handler ) {
    _via_user_input_cancel_handler();
  }
  user_input_default_cancel_handler();
}

function user_input_parse_and_invoke_handler() {
  var elist = document.getElementsByClassName('_via_user_input_variable');
  var i;
  for ( i=0; i < elist.length; ++i ) {
    var eid = elist[i].id;
    if ( _via_user_input_data.hasOwnProperty(eid) ) {
      switch(_via_user_input_data[eid].type) {
      case 'checkbox':
        _via_user_input_data[eid].value = elist[i].checked;
        break;
      default:
        _via_user_input_data[eid].value = elist[i].value;
        break;
      }
    }
  }
  if ( typeof(_via_user_input_data.confirm) !== 'undefined' ) {
    if ( _via_user_input_data.confirm.value ) {
      _via_user_input_ok_handler(_via_user_input_data);
    } else {
      if ( _via_user_input_cancel_handler ) {
        _via_user_input_cancel_handler();
      }
    }
  } else {
    _via_user_input_ok_handler(_via_user_input_data);
  }
  user_input_default_cancel_handler();
}

function show_user_input_panel() {
  document.getElementById('user_input_panel').style.display = 'block';
}

function hide_user_input_panel() {
  document.getElementById('user_input_panel').style.display = 'none';
}



//
// annotations editor panel
//
function annotation_editor_show(topx,lefty,rectIndex,recttype) {

  // remove existing annotation editor (if any)
  annotation_editor_remove();

  // create new container of annotation editor
  var ae = document.createElement('div');
  ae.setAttribute('id', 'annotation_editor');

  if ( _via_annotation_editor_mode === VIA_ANNOTATION_EDITOR_MODE.SINGLE_REGION ) {
    if ( _via_settings.ui.image.on_image_annotation_editor_placement === VIA_ANNOTATION_EDITOR_PLACEMENT.DISABLE ) {
      return;
    }


    // only display on-image annotation editor if
    // - region attribute are defined
    // - region is selected
    if ( _via_is_region_selected &&
         Object.keys(_via_attributes['region']).length &&
         _via_attributes['region'].constructor === Object ) {
      //ae.classList.add('force_small_font');
      //ae.classList.add('display_area_content'); // to enable automatic hiding of this content
      // add annotation editor to image_panel
      if ( _via_settings.ui.image.on_image_annotation_editor_placement === VIA_ANNOTATION_EDITOR_PLACEMENT.NEAR_REGION ) {
        // var html_position = annotation_editor_get_placement(_via_user_sel_region_id);
		console.log("location, topx="  + topx + ", lefty=" + lefty + "  dfdfd");
    // ae.setAttribute('style', 'top:' + topx + 'px;left:' + lefty + 'px;position:relative');
    ae.setAttribute('style', 'top:' + topx + 'px;left:' + lefty + 'px;position:relative');
      }
      _via_display_area.appendChild(ae);
      // switch (recttype)
      annotation_editor_update_content(recttype);
      update_vertical_space();
    }

  


  } 

}

function annotation_editor_hide() {
  if ( _via_annotation_editor_mode === VIA_ANNOTATION_EDITOR_MODE.SINGLE_REGION ) {
    // remove existing annotation editor (if any)
    annotation_editor_remove();
  } else {
    annotation_editor_clear_row_highlight();
  }
}


function annotation_editor_update_content(recttype) {
  return new Promise( function(ok_callback, err_callback) {
    var ae = document.getElementById('annotation_editor');
    if (ae ) {
      ae.innerHTML = '';
      annotation_editor_update_header_html();
      annotation_editor_update_metadata_html(recttype);
    }
    ok_callback();
  });
}



function annotation_editor_remove() {
  var p = document.getElementById('annotation_editor');
  if ( p ) {
    p.remove();
  }
}

function is_annotation_editor_visible() {
  return document.getElementById('annotation_editor_panel').classList.contains('display_block');
}

function annotation_editor_toggle_all_regions_editor() {
  var p = document.getElementById('annotation_editor_panel');
  if ( p.classList.contains('display_block') ) {
    p.classList.remove('display_block');
    _via_annotation_editor_mode = VIA_ANNOTATION_EDITOR_MODE.SINGLE_REGION;
  } else {
    _via_annotation_editor_mode = VIA_ANNOTATION_EDITOR_MODE.ALL_REGIONS;
    p.classList.add('display_block');
    p.style.height = _via_settings.ui.annotation_editor_height + '%';
    p.style.fontSize = _via_settings.ui.annotation_editor_fontsize + 'rem';
    annotation_editor_show();
  }
}

function annotation_editor_set_active_button() {
  var attribute_type;
  for ( attribute_type in _via_attributes ) {
    var bid = 'button_edit_' + attribute_type + '_metadata';
    document.getElementById(bid).classList.remove('active');
  }
  var bid = 'button_edit_' + _via_metadata_being_updated + '_metadata';
  document.getElementById(bid).classList.add('active');
}


function annotation_editor_update_header_html() {
  var head = document.createElement('div');
  head.setAttribute('class', 'row');
  head.setAttribute('id', 'annotation_editor_header');

  if ( _via_metadata_being_updated === 'region' ) {
    var rid_col = document.createElement('span');
    rid_col.setAttribute('class', 'col header');
    rid_col.innerHTML = '序号';
    head.appendChild(rid_col);
  }

  if ( _via_metadata_being_updated === 'file' ) {
    var rid_col = document.createElement('span');
    rid_col.setAttribute('class', 'col header');
    if ( _via_display_area_content_name === VIA_DISPLAY_AREA_CONTENT_NAME.IMAGE_GRID ) {
      rid_col.innerHTML = 'group';
    } else {
      rid_col.innerHTML = 'filename';
    }
    head.appendChild(rid_col);
  }

  var attr_id;
  for ( attr_id in _via_attributes[_via_metadata_being_updated] ) {
    var col = document.createElement('span');
    col.setAttribute('class', 'col header');
    col.innerHTML = attr_id;
    head.appendChild(col);
  }

  var ae = document.getElementById('annotation_editor');
  if ( ae.childNodes.length === 0 ) {
    ae.appendChild(head);
  } else {
    if ( ae.firstChild.id === 'annotation_editor_header') {
      ae.replaceChild(head, ae.firstChild);
    } else {
      // header node is absent
      ae.insertBefore(head, ae.firstChild);
    }
  }

  // var col = document.createElement('span');
  //   col.setAttribute('class', 'col header');
  //   col.innerHTML = "ReID自动识别";
  //   head.appendChild(col);

}

function annotation_editor_update_metadata_html(recttype) {

  var ae = document.getElementById('annotation_editor');
  switch ( _via_metadata_being_updated ) {
  case 'region':
    var rindex;
    // ae.appendChild( annotation_editor_get_metadata_row_html(_via_user_sel_region_id) )
    if ( _via_display_area_content_name === VIA_DISPLAY_AREA_CONTENT_NAME.IMAGE_GRID ) {
      ae.appendChild( annotation_editor_get_metadata_row_html(0) );
    } else {
      if ( _via_display_area_content_name === VIA_DISPLAY_AREA_CONTENT_NAME.IMAGE ) {
        if ( _via_annotation_editor_mode === VIA_ANNOTATION_EDITOR_MODE.SINGLE_REGION ) {
          ae.appendChild( annotation_editor_get_metadata_row_html(_via_user_sel_region_id,recttype) );
        } else {
          for ( rindex = 0; rindex < regions.length; ++rindex ) {
            ae.appendChild( annotation_editor_get_metadata_row_html(rindex) );
          }
        }
      }
    }
    break;

  case 'file':
    ae.appendChild( annotation_editor_get_metadata_row_html(0) );
    break;
  }
}

function annotation_editor_update_row(row_id,regons) {
  var ae = document.getElementById('annotation_editor');

  var new_row = annotation_editor_get_metadata_row_html(row_id);
  var old_row = document.getElementById(new_row.getAttribute('id'));
  ae.replaceChild(new_row, old_row);
}

function annotation_editor_add_row(row_id) {
  if ( is_annotation_editor_visible() ) {
    var ae = document.getElementById('annotation_editor');
    var new_row = annotation_editor_get_metadata_row_html(row_id);
    var penultimate_row_id = parseInt(row_id) - 1;
    if ( penultimate_row_id >= 0 ) {
      var penultimate_row_html_id = 'ae_' + _via_metadata_being_updated + '_' + penultimate_row_id;
      var penultimate_row = document.getElementById(penultimate_row_html_id);
      ae.insertBefore(new_row, penultimate_row.nextSibling);
    } else {
      ae.appendChild(new_row);
    }
  }
}

function annotation_editor_get_metadata_row_html(row_id,recttype) {
  var row = document.createElement('div');
  row.setAttribute('class', 'row');
  row.setAttribute('id', 'ae_' + _via_metadata_being_updated + '_' + row_id);

  if ( _via_metadata_being_updated === 'region' ) {
    var rid = document.createElement('span');

    switch(_via_display_area_content_name) {
    case VIA_DISPLAY_AREA_CONTENT_NAME.IMAGE_GRID:
      rid.setAttribute('class', 'col');
      rid.innerHTML = 'Grouped regions in ' + _via_image_grid_selected_img_index_list.length + ' files';
      break;
    case VIA_DISPLAY_AREA_CONTENT_NAME.IMAGE:
      rid.setAttribute('class', 'col id');
      rid.innerHTML = (row_id );
      break;
    }
    row.appendChild(rid);
  }

  if ( _via_metadata_being_updated === 'file' ) {
    var rid = document.createElement('span');
    rid.setAttribute('class', 'col');
    switch(_via_display_area_content_name) {
    case VIA_DISPLAY_AREA_CONTENT_NAME.IMAGE_GRID:
      rid.innerHTML = 'Group of ' + _via_image_grid_selected_img_index_list.length + ' files';
      break;
    case VIA_DISPLAY_AREA_CONTENT_NAME.IMAGE:
      rid.innerHTML = _via_image_filename_list[_via_image_index];
      break;
    }

    row.appendChild(rid);
  }

  var attr_id;
  for ( attr_id in _via_attributes[_via_metadata_being_updated] ) {
    var col = document.createElement('span');
    col.setAttribute('class', 'col');

    var attr_type    = _via_attributes[_via_metadata_being_updated][attr_id].type;
    var attr_desc    = _via_attributes[_via_metadata_being_updated][attr_id].desc;
    if ( typeof(attr_desc) === 'undefined' ) {
      attr_desc = '';
    }
    var attr_html_id = attr_id + '__' + row_id;

    var attr_value = '';
    var attr_placeholder = '';
    if ( _via_display_area_content_name === VIA_DISPLAY_AREA_CONTENT_NAME.IMAGE ) {
      switch(_via_metadata_being_updated) {
      case 'region':
      if (regions[row_id]!==undefined){
        if ( regions[row_id].other.region_attributes.hasOwnProperty(attr_id) ) {
          attr_value = regions[row_id].other.region_attributes[attr_id];
          if (isEmpty(attr_value) && attr_id == "id"){
            attr_value = maxIdNum + 1;
            regions[row_id].other.region_attributes[attr_id]= attr_value;
            maxIdNum = maxIdNum + 1;

          }

        } else {
          attr_placeholder = 'not defined yet!';
        }

      }

      case 'file':
         attr_placeholder = 'not defined yet!';
        // if ( _via_img_metadata[_via_image_id].file_attributes.hasOwnProperty(attr_id) ) {
        //   attr_value = _via_img_metadata[_via_image_id].file_attributes[attr_id];
        // } else {
        //   attr_placeholder = 'not defined yet!';
        // }
      }
    }


    switch(attr_type) {
    case 'text':
      col.innerHTML = '<textarea ' +
        'onchange="annotation_editor_on_metadata_update(this)" ' +
        'onfocus="annotation_editor_on_metadata_focus(this)" ' +
        'title="' + attr_desc + '" ' +
        'placeholder="' + attr_placeholder + '" ' +
        'id="' + attr_html_id + '">' + attr_value + '</textarea>';
      break;
    case 'checkbox':
      var options = _via_attributes[_via_metadata_being_updated][attr_id].options;
      var option_id;
      for ( option_id in options ) {
        var option_html_id = attr_html_id + '__' + option_id;
        var option = document.createElement('input');
        option.setAttribute('type', 'checkbox');
        option.setAttribute('value', option_id);
        option.setAttribute('id', option_html_id);
        option.setAttribute('onfocus', 'annotation_editor_on_metadata_focus(this)');
        option.setAttribute('onchange', 'annotation_editor_on_metadata_update(this)');

        var option_desc  = _via_attributes[_via_metadata_being_updated][attr_id].options[option_id];
        if ( option_desc === '' || typeof(option_desc) === 'undefined' ) {
          // option description is optional, use option_id when description is not present
          option_desc = option_id;
        }

        // set the value of options based on the user annotations
        if ( typeof attr_value !== 'undefined') {
          if ( attr_value.hasOwnProperty(option_id) ) {
            option.checked = attr_value[option_id];
          }
        }

        var label  = document.createElement('label');
        label.setAttribute('for', option_html_id);
        label.innerHTML = option_desc;

        var container = document.createElement('span');
        container.appendChild(option);
        container.appendChild(label);
        col.appendChild(container);
      }
      break;
    case 'radio':
      var option_id;
      for ( option_id in _via_attributes[_via_metadata_being_updated][attr_id].options ) {
        var option_html_id = attr_html_id + '__' + option_id;
        var option = document.createElement('input');
        option.setAttribute('type', 'radio');
        option.setAttribute('name', attr_html_id);
        option.setAttribute('value', option_id);
        option.setAttribute('id', option_html_id);
        option.setAttribute('onfocus', 'annotation_editor_on_metadata_focus(this)');
        option.setAttribute('onchange', 'annotation_editor_on_metadata_update(this)');

        var option_desc  = _via_attributes[_via_metadata_being_updated][attr_id].options[option_id];
        if ( option_desc === '' || typeof(option_desc) === 'undefined' ) {
          // option description is optional, use option_id when description is not present
          option_desc = option_id;
        }

        if ( attr_value === option_id ) {
          option.checked = true;
        }

        var label  = document.createElement('label');
        label.setAttribute('for', option_html_id);
        label.innerHTML = option_desc;

        var container = document.createElement('span');
        container.appendChild(option);
        container.appendChild(label);
        col.appendChild(container);
      }
      break;
    case 'image':
      var option_id;
      var option_count = 0;
      for ( option_id in _via_attributes[_via_metadata_being_updated][attr_id].options ) {
        option_count = option_count + 1;
      }
      var img_options = document.createElement('div');
      img_options.setAttribute('class', 'img_options');
      col.appendChild(img_options);

      var option_index = 0;
      for ( option_id in _via_attributes[_via_metadata_being_updated][attr_id].options ) {
        var option_html_id = attr_html_id + '__' + option_id;
        var option = document.createElement('input');
        option.setAttribute('type', 'radio');
        option.setAttribute('name', attr_html_id);
        option.setAttribute('value', option_id);
        option.setAttribute('id', option_html_id);
        option.setAttribute('onfocus', 'annotation_editor_on_metadata_focus(this)');
        option.setAttribute('onchange', 'annotation_editor_on_metadata_update(this)');

        var option_desc  = _via_attributes[_via_metadata_being_updated][attr_id].options[option_id];
        if ( option_desc === '' || typeof(option_desc) === 'undefined' ) {
          // option description is optional, use option_id when description is not present
          option_desc = option_id;
        }

        if ( attr_value === option_id ) {
          option.checked = true;
        }

        var label  = document.createElement('label');
        label.setAttribute('for', option_html_id);
        label.innerHTML = '<img src="' + option_desc + '"><p>' + option_id + '</p>';

        var container = document.createElement('span');
        container.appendChild(option);
        container.appendChild(label);
        img_options.appendChild(container);
      }
      break;

    case 'dropdown':
      var sel = document.createElement('select');
      sel.setAttribute('id', attr_html_id);
      sel.setAttribute('onfocus', 'annotation_editor_on_metadata_focus(this)');
      sel.setAttribute('onchange', 'annotation_editor_on_metadata_update(this)');
      var option_id;
      var option_selected = false;
      for ( option_id in _via_attributes[_via_metadata_being_updated][attr_id].options ) {
        var option_html_id = attr_html_id + '__' + option_id;
        var option = document.createElement('option');
        option.setAttribute('value', option_id);

        var option_desc  = _via_attributes[_via_metadata_being_updated][attr_id].options[option_id];
        if ( option_desc === '' || typeof(option_desc) === 'undefined' ) {
          // option description is optional, use option_id when description is not present
          option_desc = option_id;
        }

        if ( option_id === attr_value ) {
          option.setAttribute('selected', 'selected');
          option_selected = true;
        }
        option.innerHTML = option_desc;
        sel.appendChild(option);
      }

      if ( ! option_selected ) {
        sel.selectedIndex = '-1';
      }
      col.appendChild(sel);
      break;

    }

    row.appendChild(col);
  }
  var col = document.createElement('span');
  col.setAttribute('class', 'col');
  var reId_button = document.createElement('button');
  reId_button.setAttribute('style', 'border-radius:8px');
 
  // reId_button.innerHTML = "点击查看";
  //   reId_button.onclick=function () { 
  //   showImage(row_id,recttype);
  //   }
  // col.appendChild(reId_button);
  // row.appendChild(col);
  return row;
}

function annotation_editor_scroll_to_row(row_id) {
  if ( is_annotation_editor_visible() ) {
    var row_html_id = 'ae_' + _via_metadata_being_updated + '_' + row_id;
    var row = document.getElementById(row_html_id);
    row.scrollIntoView(false);
  }
}

function annotation_editor_highlight_row(row_id) {
  if ( is_annotation_editor_visible() ) {
    var row_html_id = 'ae_' + _via_metadata_being_updated + '_' + row_id;
    var row = document.getElementById(row_html_id);
    row.classList.add('highlight');
  }
}

function annotation_editor_clear_row_highlight() {
  if ( is_annotation_editor_visible() ) {
    var ae = document.getElementById('annotation_editor');
    var i;
    for ( i=0; i<ae.childNodes.length; ++i ) {
      ae.childNodes[i].classList.remove('highlight');
    }
  }
}

function annotation_editor_extract_html_id_components(html_id) {
  // html_id : attribute_name__row-id__option_id
  var parts = html_id.split('__');
  var parsed_id = {};
  switch( parts.length ) {
  case 3:
    // html_id : attribute-id__row-id__option_id
    parsed_id.attr_id = parts[0];
    parsed_id.row_id  = parts[1];
    parsed_id.option_id = parts[2];
    break;
  case 2:
    // html_id : attribute-id__row-id
    parsed_id.attr_id = parts[0];
    parsed_id.row_id  = parts[1];
    break;
  default:
  }
  return parsed_id;
}


// invoked when the input entry in annotation editor receives focus
function annotation_editor_on_metadata_focus(p) {
  if ( _via_annotation_editor_mode === VIA_ANNOTATION_EDITOR_MODE.ALL_REGIONS ) {
    var pid       = annotation_editor_extract_html_id_components(p.id);
    var region_id = pid.row_id;
    // clear existing highlights (if any)
    toggle_all_regions_selection(false);
    annotation_editor_clear_row_highlight();
    // set new selection highlights
    set_region_select_state(region_id, true);
    annotation_editor_scroll_to_row(region_id);
    annotation_editor_highlight_row(region_id);

    // _via_redraw_reg_canvas();
  }
}

// invoked when the user updates annotations using the annotation editor
function annotation_editor_on_metadata_update(p) {
  var pid       = annotation_editor_extract_html_id_components(p.id);
  var img_id    = _via_image_id;

  var img_index_list = [ _via_image_index ];
  var region_id = pid.row_id;
  if ( _via_display_area_content_name === VIA_DISPLAY_AREA_CONTENT_NAME.IMAGE_GRID ) {
    img_index_list = _via_image_grid_selected_img_index_list.slice(0);
    region_id = -1; // this flag denotes that we want to update all regions
  }



  if ( _via_metadata_being_updated === 'region' ) {
      if (pid.attr_id == "id"){//判断id输入是否为数字
          if(!isNumber(p.value)){
             alert('请输入有效的数字');
             return;
          }
      }
      annotation_editor_update_region_metadata( region_id, pid.attr_id, p.value, p.checked).then( function(update_count) {
      if (p.value > maxIdNum && pid.attr_id == "id"){
        maxIdNum = parseInt(p.value);
      }
      // annotation_editor_on_metadata_update_done('region', pid.attr_id, update_count);
	  // console.log('aaaaaaaaaaaaaaaa');
	  // updateLabelHtml();
	  
    }, function(err) {
      show_message('Failed to update region attributes! ');
    });
    return;
  }
}



function isNumber(value) {         //验证是否为数字
    var patrn = /^(-)?\d+(\.\d+)?$/;
    if (patrn.exec(value) == null || value == "") {
       return false
   } else {
       return true
   }
}

function annotation_editor_update_region_metadata( region_id, attr_id, new_value, new_checked) {
  return new Promise( function(ok_callback, err_callback) {
    var i, n, img_id, img_index;
    // n = img_index_list.length;
    var update_count = 0;
    var region_list = [];
    var j, m;

    if ( region_id === -1 ) {
      // update all regions on a file (for image grid view)
      // for ( i = 0; i < n; ++i ) {
        // img_index = img_index_list[i];
        // img_id = _via_image_id_list[img_index];

        m = regions.length;
        for ( j = 0; j < m; ++j ) {
          // if ( ! image_grid_is_region_in_current_group(regions[j].region_attributes ) ) {
          //   continue;
          // }

          switch( _via_attributes['region'][attr_id].type ) {
          case 'text':  // fallback
          case 'dropdown': // fallback
          case 'radio': // fallback
          // case 'image':
           
            regions[j].other.region_attributes[attr_id] = new_value;
            update_count += 1;
            break;
          case 'checkbox':
            var option_id = new_value;
            if ( regions[j].other.region_attributes.hasOwnProperty(attr_id) ) {
              if ( typeof(regions[j].other.region_attributes[attr_id]) !== 'object' ) {
                var old_value = regions[j].other.region_attributes[attr_id];
                regions[j].other.region_attributes[attr_id] = {}
                if ( Object.keys(_via_attributes['region'][attr_id]['options']).includes(old_value) ) {
                  // transform existing value as checkbox option
                 regions[j].other.region_attributes[attr_id][old_value] = true;
                }
              }
            } else {
              regions[j].other.region_attributes[attr_id] = {};
            }

            if ( new_checked ) {
              regions[j].other.region_attributes[attr_id][option_id] = true;
            } else {
              // false option values are not stored
              delete regions[j].other.region_attributes[attr_id][option_id];
            }
            update_count += 1;
            break;
          }
        }
      // }
    } else {

        switch( _via_attributes['region'][attr_id].type ) {
        case 'text':  // fallback
        case 'dropdown': // fallback
        case 'radio': // fallback
        case 'image':
        // if (attr_id == "id"){
        //       if(!isNumber(new_value)){
        //          alert('不是有效的数字');
        //          continue;
        //       }
        //   }
          regions[region_id].other.region_attributes[attr_id] = new_value;
          update_count += 1;
          break;
        case 'checkbox':
          var option_id = new_value;

          if ( regions[region_id].other.region_attributes.hasOwnProperty(attr_id) ) {
            if ( typeof(regions[region_id].other.region_attributes[attr_id]) !== 'object' ) {
              var old_value = regions[region_id].other.region_attributes[attr_id];
              [region_id].other.region_attributes[attr_id] = {};
              if ( Object.keys(_via_attributes['region'][attr_id]['options']).includes(old_value) ) {
                // transform existing value as checkbox option
                regions[region_id].other.region_attributes[attr_id][old_value] = true;
              }
            }
          } else {
            regions[region_id].other.region_attributes[attr_id] = {};
          }

          if ( new_checked ) {
            regions[region_id].other.region_attributes[attr_id][option_id] = true;
          } else {
            // false option values are not stored
            delete regions[region_id].other.region_attributes[attr_id][option_id];
          }
          update_count += 1;
          break;
        }
      // }
    }
    ok_callback(update_count);
  });
}

function set_region_annotations_to_default_value(rid) {
  var attr_id;
  for ( attr_id in _via_attributes['region'] ) {
    var attr_type = _via_attributes['region'][attr_id].type;
    switch( attr_type ) {
    case 'text':
      var default_value = _via_attributes['region'][attr_id].default_value;
      if ( typeof(default_value) !== 'undefined' ) {
        regions[rid].other.region_attributes[attr_id] = default_value;
      }
      break;
    case 'image':    // fallback
    case 'dropdown': // fallback
    case 'radio':
      regions[rid].other.region_attributes[attr_id] = '';
      var default_options = _via_attributes['region'][attr_id].default_options;
      if ( typeof(default_options) !== 'undefined' ) {
        regions[rid].other.region_attributes[attr_id] = Object.keys(default_options)[0];
      }
      break;

    case 'checkbox':
      regions[rid].other.region_attributes[attr_id] = {};
      var default_options = _via_attributes['region'][attr_id].default_options;
      if ( typeof(default_options) !== 'underfined' ) {
        var option_id;
        for ( option_id in default_options ) {
          var default_value = default_options[option_id];
          if ( typeof(default_value) !== 'underfined' ) {
            regions[rid].other.region_attributes[attr_id][option_id] = default_value;
          }
        }
      }
      break;
    }
  }
}


function show_tpye_attribute(){
  sessionStorage.setItem('attr_id','type'); 
  // sessionStorage.setItem(\'predict_task_id\',\'"+task_id +"\');
  show_attribute_properties(); 
  show_tpye_attribute_options();

}

function show_color_attribute(){
 
  sessionStorage.setItem('attr_id','color'); 
  var isTrue = show_attribute_properties_color(); 
  if (!isTrue){
    return;
  }
  show_tpye_attribute_options();

}

function show_tpye_attribute_options() {
  // var attr_list = document.getElementById('attributes_name_list');
  document.getElementById('attribute_options').innerHTML = '';
   document.getElementById('attribute_options').innerHTML = '';


  // var attr_id = attr_list.value;
  var attr_id = sessionStorage.getItem("attr_id");
  var attr_type = _via_attributes[_via_attribute_being_updated][attr_id].type;

  // populate additional options based on attribute type
  switch( attr_type ) {
  case VIA_ATTRIBUTE_TYPE.TEXT:
    // text does not have any additional properties
    break;
  case VIA_ATTRIBUTE_TYPE.CHECKBOX: // handled by next case
  case VIA_ATTRIBUTE_TYPE.DROPDOWN: // handled by next case
  case VIA_ATTRIBUTE_TYPE.RADIO:
    var p = document.createElement('div');
    p.setAttribute('class', 'property');
    p.setAttribute('style', 'text-align:center');
    var c0 = document.createElement('span');
    c0.setAttribute('style', 'width:25%');
    c0.setAttribute('title', 'When selected, this is the value that appears in exported annotations');
    c0.innerHTML = 'id';
    var c1 = document.createElement('span');
    c1.setAttribute('style', 'width:60%');
    c1.setAttribute('title', 'This is the text shown as an option to the annotator');
    c1.innerHTML = 'description';
    var c2 = document.createElement('span');
    c2.setAttribute('title', 'The default value of this attribute');
    c2.innerHTML = 'def.';
    p.appendChild(c0);
    p.appendChild(c1);
    p.appendChild(c2);
    document.getElementById('attribute_options').appendChild(p);

    input_type_attibutes(attr_id);
    var options = _via_attributes[_via_attribute_being_updated][attr_id].options;

    var option_id;
    for ( option_id in options ) {
      var option_desc = options[option_id];

      var option_default = _via_attributes[_via_attribute_being_updated][attr_id].default_options[option_id];
      attribute_property_add_option(attr_id, option_id, option_desc, option_default, attr_type);
    }
    attribute_property_add_new_entry_option(attr_id, attr_type);
    break;
  default:
    console.log('Attribute type ' + attr_type + ' is unavailable');
  }
}


function input_type_attibutes(attr_id){
  _via_attributes[_via_attribute_being_updated][attr_id]["description"] = "";
  _via_attributes[_via_attribute_being_updated][attr_id]["options"] = {};
  if (attr_id=="type"){
     _via_attributes[_via_attribute_being_updated][attr_id]["options"] = {"1":"轿车", "2":"SUV","3":"越野车","4":"出租车","5":"商务车","6":"载人面包车","7":"军警车-轿车","8":"军警车-SUV","9":"军警车-越野车","10":"军警车-商务车","11":"军警车-载人面包车","12":"军警车-客车",13:"中型客车","14":"公交车","15":"大型客车","16":"货用面包车","17":"厢式货车","18":"微型货车","19":"皮卡车","20":"救援车","21":"大型货车","22":"渣土车","23":"挂车","24":"罐车","25":"混凝土搅拌车","26":"随车吊","27":"救护车","28":"三轮车","29":"其他"};
  }
  else if (attr_id=="color"){
     _via_attributes[_via_attribute_being_updated][attr_id]["options"] = {"1":"黑", "2":"白","3":"灰","4":"红","5":"蓝","6":"黄","7":"橙","8":"棕","9":"绿","10":"紫","11":"青","12":"粉",13:"银","14":"金","15":"混色","16":"其他","17":"未知"};
  }
 
  _via_attributes[_via_attribute_being_updated][attr_id]["default_options"] = {};
}


function judge_exit_color(){
    var tableId = document.getElementById('attributes_name_list');
    for(var i=1;i<tableId.rows.length;i++) { 
        // alert(tableId.rows[i].cells[1].innerHTML); 
        if (tableId.rows[i].cells[0].innerHTML == "color"){
          return true;
        }
      } 
    return false;
} 


function show_attribute_properties_color() {
  if (!judge_exit_color()){
    alert("请输入属性值：color.");
    return false;
  }
    
  // var attr_list = document.getElementById('attributes_name_list');
  document.getElementById('attribute_properties').innerHTML = '';

  var attr_id = sessionStorage.getItem("attr_id");
    
  var attr_type = _via_attribute_being_updated;
  _via_attributes[attr_type][attr_id].type = "dropdown"
  var attr_input_type = _via_attributes[attr_type][attr_id].type;
  var attr_desc = _via_attributes[attr_type][attr_id].description;

  attribute_property_add_input_property('Name of attribute (appears in exported annotations)',
                                        'Name',
                                        attr_id,
                                        'attribute_name');
  attribute_property_add_input_property('Description of attribute (shown to user during annotation session)',
                                        'Desc.',
                                        attr_desc,
                                        'attribute_description');

  if ( attr_input_type === 'text' ) {
    var attr_default_value = _via_attributes[attr_type][attr_id].default_value;
    attribute_property_add_input_property('Default value of this attribute',
                                          'Def.',
                                          attr_default_value,
                                          'attribute_default_value');
  }

  // add dropdown for type of attribute
  var p = document.createElement('div');
  p.setAttribute('class', 'property');
  var c0 = document.createElement('span');
  c0.setAttribute('title', 'Attribute type (e.g. text, checkbox, radio, etc)');
  c0.innerHTML = 'Type';
  var c1 = document.createElement('span');
  var c1b = document.createElement('select');
  c1b.setAttribute('onchange', 'attribute_property_on_update(this)');
  c1b.setAttribute('id', 'attribute_type');
  var type_id;
  for ( type_id in VIA_ATTRIBUTE_TYPE ) {
    var type = VIA_ATTRIBUTE_TYPE[type_id];
    var option = document.createElement('option');
    option.setAttribute('value', type);
    option.innerHTML = type;
    if ( attr_input_type == type ) {
      option.setAttribute('selected', 'selected');
    }
    c1b.appendChild(option);
  }
  c1.appendChild(c1b);
  p.appendChild(c0);
  p.appendChild(c1);
  document.getElementById('attribute_properties').appendChild(p);
  return true;
}
