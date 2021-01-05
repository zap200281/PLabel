import EventEmitter from 'tiny-emitter';
import OpenSeadragon from 'openseadragon';
import { SVG_NAMESPACE } from '../SVGConst';
import { DrawingTools, drawShape, parseRectFragment } from '@recogito/annotorious';

export default class OSDAnnotationLayer extends EventEmitter {

  constructor(props) {
    super();

    this.viewer = props.viewer;

    this.readOnly = props.readOnly;

    this.svg = document.createElementNS(SVG_NAMESPACE, 'svg');
    this.svg.classList.add('a9s-annotationlayer', 'a9s-osd-annotationlayer');

    this.g = document.createElementNS(SVG_NAMESPACE, 'g');
    this.svg.appendChild(this.g);

    this.viewer.canvas.appendChild(this.svg);

    this.viewer.addHandler('animation', () => this.resize());
    this.viewer.addHandler('open', () => this.resize());
    this.viewer.addHandler('rotate', () => this.resize());
    this.viewer.addHandler('resize', () => this.resize());

    this.selectedShape = null;

    if (!this.readOnly) {
      this.tools = new DrawingTools(this.g);
      this._initDrawingMouseTracker();
    }

    this.resize();
  }

  /** Initializes the OSD MouseTracker used for drawing **/
  _initDrawingMouseTracker = () => {
    this.tools.on('complete', shape => { 
      this.mouseTracker.setTracking(false);
      this.selectShape(shape);
    });

    this.mouseTracker = new OpenSeadragon.MouseTracker({
      element: this.svg,

      // Keypress starts drawing
      pressHandler:  evt => {
        this.tools.current.startDrawing(evt.originalEvent);
      },

      // Move updates the tool (if drawing)
      moveHandler: evt => {
        if (this.tools.current.isDrawing)
          this.tools.current.onMouseMove(evt.originalEvent);
      },

      // Stops drawing
      releaseHandler: evt => {
        this.tools.current.onMouseUp(evt.originalEvent);
      }
    }).setTracking(false);

    // Keep tracker disabled until Shift is held
    document.addEventListener('keydown', evt => {
      if (evt.which === 16) // Shift
        this.mouseTracker.setTracking(true);
    });

    document.addEventListener('keyup', evt => {
      if (evt.which === 16 && !this.tools.current.isDrawing)
        this.mouseTracker.setTracking(false);
    });
  }

  addAnnotation = annotation => {
    const shape = drawShape(annotation);
    shape.setAttribute('class', 'a9s-annotation');
    shape.setAttribute('data-id', annotation.id);
    shape.annotation = annotation;

    shape.addEventListener('mouseenter', evt => {
      if (!this.tools.current.isDrawing)
        this.emit('mouseEnterAnnotation', annotation, evt);
    });

    shape.addEventListener('mouseleave', evt => {
      if (!this.tools.current.isDrawing)
        this.emit('mouseLeaveAnnotation', annotation, evt);
    });

    new OpenSeadragon.MouseTracker({
      element: shape,
      clickHandler: () => this.selectShape(shape)
    }).setTracking(true);

    this.g.appendChild(shape);
  }

  findShape = annotationOrId => {
    const id = annotationOrId?.id ? annotationOrId.id : annotationOrId;
    return this.g.querySelector(`.a9s-annotation[data-id="${id}"]`);
  }

  selectShape = (shape, skipEvent) => {
    // If another shape is currently selected, deselect first
    if (this.selectedShape && this.selectedShape.annotation !== shape.annotation)
      this.deselect();

    const { annotation } = shape;

    this.selectedShape = shape;

    this.emit('select', { annotation, element: shape, skipEvent }); 
  }

  init = annotations => {
    const shapes = Array.from(this.g.querySelectorAll('.a9s-annotation'));
    shapes.forEach(s => this.g.removeChild(s));
    annotations.forEach(this.addAnnotation);
  }
  
  resize() {
    // Current upper left corner
    const p = this.viewer.viewport.pixelFromPoint(new OpenSeadragon.Point(0, 0), true);

    // Compute scale factor
    const { x, y } = this.viewer.viewport.getContainerSize();
    const containerSize = Math.max(x, y);
    const zoom = this.viewer.viewport.getZoom(true);
    const scale = zoom * containerSize / this.viewer.world.getContentFactor();

    const rotation = this.viewer.viewport.getRotation();

    this.g.setAttribute('transform', `translate(${p.x}, ${p.y}) scale(${scale}) rotate(${rotation})`);

    if (this.selectedShape)
      this.emit('moveSelection', this.selectedShape);
  }

  deselect = () => {
    if (this.selectedShape?.annotation.isSelection)
      this.tools.current.stop();

    this.selectedShape = null;
  }

  addOrUpdateAnnotation = (annotation, previous) => {
    if (previous)
      this.removeAnnotation(annotation);

    this.addAnnotation(annotation);
  }

  removeAnnotation = annotation => {
    if (this.selectedShape?.annotation === annotation)
      this.deselect();

    const shape = this.findShape(annotation);
    if (shape)
      shape.parentNode.removeChild(shape);
  }

  selectAnnotation = annotationOrId => {
    // Deselect first
    if (this.selectedShape)
      this.deselect();

    const selected = this.findShape(annotationOrId);

    // Select with 'skipEvent' flag
    if (selected)
      this.selectShape(selected, true);
    else
      this.deselect();

    return selected?.annotation;
  }

  panTo = (annotationOrId, immediately) => {
    const shape = this.findShape(annotationOrId);
    if (shape) {
      const { top, left, width, height } = shape.getBoundingClientRect();

      const x = left + width / 2 + window.scrollX;
      const y = top + height / 2 + window.scrollY;
      const center = this.viewer.viewport.windowToViewportCoordinates(new OpenSeadragon.Point(x, y));

      this.viewer.viewport.panTo(center, immediately);
    }    
  }

  fitBounds = (annotationOrId, immediately) => {
    const shape = this.findShape(annotationOrId);
    if (shape) {
      const { x, y, w, h } = parseRectFragment(shape.annotation);      
      const rect = this.viewer.viewport.imageToViewportRectangle(x, y, w, h);
      
      this.viewer.viewport.fitBounds(rect, immediately);
    }    
  }

  setDrawingTool = shape =>
    this.tools.setCurrent(shape);

  getAnnotations = () => {
    const shapes = Array.from(this.g.querySelectorAll('.a9s-annotation'));
    return shapes.map(s => s.annotation);
  }

  destroy = () => {
    this.selectedShape = null;
    this.svg.parentNode.removeChild(this.svg);
  }

}