# blox

A Clojure library designed to create parametric 3d models. 

It's inspired by https://github.com/farrellm/scad-clj but the emphasis is on composable functions rather than macros. 

## Usage

The simplest way to try this out is to execute the example functions tx0 .. tx4 towards the end of the scad.clj file. These generate .scad files in the out directory which can be viewed with the Openscad software available from [OpenSCAD.org](http://www.openscad.org/index.html). Or you can just take a look at these pictures:

t0 - an odd shape with some translucent parts:

![t0.clj - an odd shape with some translucent parts](out/t0.png)

t1 - a sphere with a cylinder subtracted:

![t1 - a sphere with a cylinder subtracted](out/t1.png)

t2 - Jackstone?

![t2 - - Jackstone?](out/t2.png)

t3 - the interior of a Jackstone:

![t3](out/t3.png)

t4 - I have no idea what to call this, it's a hull around a scaled jackstone:

![t4](out/t4.png)

The other file, brick.clj, is an example of using the convenience functions (currently limited to cube, cylinder, difference, translate and union. The brick and plate functions generate something that looks like a lego component. Please note that a 3d printed version of this is almost certainly not going to actually be lego compatible. The interior of the brick is not quite right and the measurements are off. 

(brick 3 4) - an impossible brick:

![3x4 exterior](out/3x4 exterior.png)

(brick 3 4) - a view of the interior:

![3x4 interior](out/3x4 interior.png)



## License

Copyright A. Dwelly © 2017

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
