<p align="center">
  <img width="345" src="https://raw.githubusercontent.com/recogito/annotorious-openseadragon/master/annotorious-osd-stylized.png" />
  <br/><br/>
</p>

A plugin for [OpenSeadragon](https://openseadragon.github.io/) that integrates [Annotorious](https://github.com/recogito/annotorious)
to enable creation and display of annotations on high-resolution zoomable images. See the 
[project website](https://recogito.github.io/annotorious/) for details and live demos.

## Installing

Download the [latest release](https://github.com/recogito/annotorious-openseadragon/releases/latest)
and include it in your web page __after__ the OpenSeadragon script.

```html
<script src="openseadragon/openseadragon.2.4.2.min.js"></script>
<script src="openseadragon-annotorious.min.js"></script>
```

## Using

```html
<script>
  window.onload = function() {
    var viewer = OpenSeadragon({
      id: "openseadragon1",
      prefixUrl: "openseadragon/images/",
      tileSources: {
        type: "image",
        url: "1280px-Hallstatt.jpg"
      }
    });

    // Initialize the Annotorious plugin
    var anno = OpenSeadragon.Annotorious(viewer);

    // Load annotations in W3C WebAnnotation format
    anno.loadAnnotations('annotations.w3c.json');

    // Attach handlers to listen to events
    anno.on('createAnnotation', function(a) {
      // Do something
    });
  }
</script>
```

Full documentation is [on the project website](https://recogito.github.io/annotorious/). Questions? Feedack? Feature requests? Join the [Annotorious chat on Gitter](https://gitter.im/recogito/annotorious).

[![Join the chat at https://gitter.im/recogito/annotorious](https://badges.gitter.im/recogito/annotorious.svg)](https://gitter.im/recogito/annotorious?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## License

[BSD 3-Clause](LICENSE) (= feel free to use this code in whatever way
you wish. But keep the attribution/license file, and if this code
breaks something, don't complain to us :-)


