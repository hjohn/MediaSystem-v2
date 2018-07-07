



Presentations and Views are bidirectionally linked.

A specific Presentation refers to a View, but not always the same one:

PlayerPresentation -> VLCPlayer
                   -> MPLayer
                   
Although it can refer to the same type of view:

PlayerPresentation -> Player (interface)
EditorPresentation -> Node (javafx generic class)                  
                   
A view does never directly refer to a presentation, instead it is bound
by the layout to a presentation (or multiple presentations).  It is entirely
possible to re-use a view with a different presentation class or multiple
classes.  For example, a view with 2 controls could have both controls bound
to a single presentation or have each control bound to two seperate
presentations at the layout's disgression.

A Layout is static and creates a single Presentation + View combination.

The same presentation cannot be bound to multiple Views (due to bidirectional
bindings), however they're lightweight and are constructed at the same time
as the associated view is by the layout.

Summarize:

- A presentation refers to a view of a generic type
  => A presentation can return the (generic) view class it is bound to

- A view only indirectly refers to the presentation through bindings
  => It is an error to supply a View with a presentation
  
- A layout is a static factory for a specific presentation + view combination
