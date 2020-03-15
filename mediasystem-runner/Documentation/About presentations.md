# Presentations

Presentations are classes that contain state and methods that act upon this state.  They 
donot contain controls are any other representation code.

Presentations can exist after a View is no longer displayed, and can be re-associated 
with a View later to recreate a View in the exact same state as it was before.  The 
View creates weak bindings to a Presentation, and a single Presentation could potentially be 
associated with multiple views.

It is always an error to create strong bindings to a Presentation, as Presentations are
long-lived objects and could potentially keep old Views in memory even after they have are
no longer displayed.

# Views

Views represent the content of a Presentation on a screen or in a control.  They contain
logic for representating the data in the Presentation, and handle graphic effects and the
look and feel of controls.

All JavaFX controls are simple Views with properties that can be bound to a data source.
A View is nothing more than a group of such controls that is bound to a Presentation,
effectively binding many individual controls at once.

A View can keep a reference to an associated Presentation, but must take care to use
weak bindings when binding directly to values in the presentation.  Bindings use listeners
that are kept track of by the Presentation, and if they were to be strongly referenced
these listeners would cause the View to not be able to be garbage collected for as long
as the Presentation exists.  
