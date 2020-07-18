# Help

### Navigation

MediaSystem is fully keyboard controlled.  Use the arrows keys to navigate to different elements and press
`Enter` to select an element. `Backspace` is used to navigate back to previous pages or to exit dialogs. 
When navigating lists of items, the `Page Up`, `Page Down`, `Home` and `End` keys can also be used to skip
multiple items at once or to navigate to the start or end of the list.

On most screens a menu with more options can be opened by using the `F10` key.  This menu contains
additional options relevant to the current activity, like sorting and filtering, or controls like
brightness and playback speed.  Also some screens contain quick options marked with a red, green, yellow
or blue dot (similar to those found on most media remotes).  These can be controlled with the keys `F1`, 
`F2`, `F3` and `F4` respectively.  Combining these with the `Alt` key can trigger an alternate action.

During playback, the arrow keys can be used to skip forward or backward.  The `Right` and `Left` arrow keys
by default skip 10 seconds forwards or backwards, while the `Up` and `Down` arrow keys skip one minute
forward or backwards.  The `Space` key can be used to pause playback.  The `I` key can be used to toggle
visibility of the playback position and time indicators.

The configuration file `mediasystem.yaml` contains all keyboard mappings.

### Configuration

MediaSystem can be configured to import your collection of videos in various ways.  Modify `mediasystem-imports.yaml`
to configure from which folders MediaSystem should import, what the folder represents (Movies or Series) and how
it should be tagged.

The file `mediasystem-collections.yaml` can in turn be used to set up one or more collections.  A collection has a title,
a tag which selects its content and a type.  The tag selects which videos appear in the collection, and its type
how it will be displayed.  The available types are `Movie`, `Serie` and `Folder`</b>.
