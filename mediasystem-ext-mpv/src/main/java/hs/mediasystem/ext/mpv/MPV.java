package hs.mediasystem.ext.mpv;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.List;
import java.util.Map;

public interface MPV extends Library {
  MPV INSTANCE = Native.load("mpv", MPV.class, Map.of(Library.OPTION_STRING_ENCODING, "UTF-8"));  // FIXME works on linux, now make it work for windows again

  /*
   * Event ID's
   */

  int MPV_EVENT_NONE = 0;         // Notification when time out expires
  int MPV_EVENT_START_FILE = 6;   // Notification before playback start of a file (before the file is loaded).
  int MPV_EVENT_END_FILE = 7;     // Notification after playback end (after the file was unloaded).
  int MPV_EVENT_FILE_LOADED = 8;  // Notification when the file has been loaded (headers were read etc.), and decoding starts.

  /**
   * Sent every time after a video frame is displayed. Note that currently,
   * this will be sent in lower frequency if there is no video, or playback
   * is paused - but that will be removed in the future, and it will be
   * restricted to video frames only.
   *
   * @deprecated Use mpv_observe_property() with relevant properties instead
   *             (such as "playback-time").
   */
  @Deprecated
  int MPV_EVENT_TICK = 14;

  /**
   * Event sent due to mpv_observe_property().
   * See also mpv_event and mpv_event_property.
   */
  int MPV_EVENT_PROPERTY_CHANGE = 22;

  /*
   * Error codes
   */

  /**
   * The accessed property doesn't exist.
   */
  int MPV_ERROR_PROPERTY_NOT_FOUND = -8;

  long mpv_client_api_version();
  long mpv_create();

  /**
   * Disconnect and destroy the mpv_handle. ctx will be deallocated with this
   * API call.
   *
   * If the last mpv_handle is detached, the core player is destroyed. In
   * addition, if there are only weak mpv_handles (such as created by
   * mpv_create_weak_client() or internal scripts), these mpv_handles will
   * be sent MPV_EVENT_SHUTDOWN. This function may block until these clients
   * have responded to the shutdown event, and the core is finally destroyed.
   */
  void mpv_destroy(long handle);

  int mpv_initialize(long handle);
  int mpv_command(long handle, String[] args);
  int mpv_command_string(long handle, String args);
  Pointer mpv_get_property_string(long handle, String name);
  int mpv_set_property_string(long handle, String name, String data);
  int mpv_set_option_string(long handle, String name, String data);
  void mpv_free(Pointer data);
  int mpv_set_option(long handle, String name, int format, Pointer data);

  /**
   * Wait for the next event, or until the timeout expires, or if another thread
   * makes a call to mpv_wakeup(). Passing 0 as timeout will never wait, and
   * is suitable for polling.
   *
   * The internal event queue has a limited size (per client handle). If you
   * don't empty the event queue quickly enough with mpv_wait_event(), it will
   * overflow and silently discard further events. If this happens, making
   * asynchronous requests will fail as well (with MPV_ERROR_EVENT_QUEUE_FULL).
   *
   * Only one thread is allowed to call this on the same mpv_handle at a time.
   * The API won't complain if more than one thread calls this, but it will cause
   * race conditions in the client when accessing the shared mpv_event struct.
   * Note that most other API functions are not restricted by this, and no API
   * function internally calls mpv_wait_event(). Additionally, concurrent calls
   * to different mpv_handles are always safe.
   *
   * As long as the timeout is 0, this is safe to be called from mpv render API
   * threads.
   *
   * @param timeout Timeout in seconds, after which the function returns even if
   *                no event was received. A MPV_EVENT_NONE is returned on
   *                timeout. A value of 0 will disable waiting. Negative values
   *                will wait with an infinite timeout.
   * @return A struct containing the event ID and other data. The pointer (and
   *         fields in the struct) stay valid until the next mpv_wait_event()
   *         call, or until the mpv_handle is destroyed. You must not write to
   *         the struct, and all memory referenced by it will be automatically
   *         released by the API on the next mpv_wait_event() call, or when the
   *         context is destroyed. The return value is never NULL.
   */
  mpv_event mpv_wait_event(long handle, double timeOut);

  /**
   * Enable or disable the given event.
   *
   * Some events are enabled by default. Some events can't be disabled.
   *
   * (Informational note: currently, all events are enabled by default, except
   * {@value #MPV_EVENT_TICK}.)
   *
   * Safe to be called from mpv render API threads.
   *
   * @param handle mpv handle
   * @param event_id See enum mpv_event_id.
   * @param enable 1 to enable receiving this event, 0 to disable it.
   * @return error code
   */
  int mpv_request_event(long handle, int event_id, int enable);

  /**
   * Get a notification whenever the given property changes. You will receive
   * updates as MPV_EVENT_PROPERTY_CHANGE. Note that this is not very precise:
   * for some properties, it may not send updates even if the property changed.
   * This depends on the property, and it's a valid feature request to ask for
   * better update handling of a specific property. (For some properties, like
   * ``clock``, which shows the wall clock, this mechanism doesn't make too
   * much sense anyway.)
   *
   * Property changes are coalesced: the change events are returned only once the
   * event queue becomes empty (e.g. mpv_wait_event() would block or return
   * MPV_EVENT_NONE), and then only one event per changed property is returned.
   *
   * You always get an initial change notification. This is meant to initialize
   * the user's state to the current value of the property.
   *
   * Normally, change events are sent only if the property value changes according
   * to the requested format. mpv_event_property will contain the property value
   * as data member.
   *
   * Warning: if a property is unavailable or retrieving it caused an error,
   *          MPV_FORMAT_NONE will be set in mpv_event_property, even if the
   *          format parameter was set to a different value. In this case, the
   *          mpv_event_property.data field is invalid.
   *
   * If the property is observed with the format parameter set to MPV_FORMAT_NONE,
   * you get low-level notifications whether the property _may_ have changed, and
   * the data member in mpv_event_property will be unset. With this mode, you
   * will have to determine yourself whether the property really changed. On the
   * other hand, this mechanism can be faster and uses less resources.
   *
   * Observing a property that doesn't exist is allowed. (Although it may still
   * cause some sporadic change events.)
   *
   * Keep in mind that you will get change notifications even if you change a
   * property yourself. Try to avoid endless feedback loops, which could happen
   * if you react to the change notifications triggered by your own change.
   *
   * Only the mpv_handle on which this was called will receive the property
   * change events, or can unobserve them.
   *
   * Safe to be called from mpv render API threads.
   *
   * @param handle         mpv handle
   * @param reply_userdata This will be used for the mpv_event.reply_userdata
   *                       field for the received MPV_EVENT_PROPERTY_CHANGE
   *                       events. (Also see section about asynchronous calls,
   *                       although this function is somewhat different from
   *                       actual asynchronous calls.)
   *                       If you have no use for this, pass 0.
   *                       Also see mpv_unobserve_property().
   * @param name The property name.
   * @param format see enum mpv_format. Can be MPV_FORMAT_NONE to omit values
   *               from the change events.
   * @return error code (usually fails only on OOM or unsupported format)
   */
  int mpv_observe_property(long handle, long reply_userdata, String name, int format);

  class mpv_event extends Structure {
    private static final List<String> FIELD_ORDER = List.of("event_id", "error", "reply_userdata", "data");

    public int event_id;
    public int error;
    public long reply_userdata;
    public Pointer data;

    @Override
    protected List<String> getFieldOrder() {
      return FIELD_ORDER;
    }
  }
}