package hs.mediasystem.ext.mpv;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;

import java.util.List;
import java.util.Map;

public interface MPV extends StdCallLibrary {
  MPV INSTANCE = Native.load("lib/mpv-1.dll", MPV.class, Map.of(Library.OPTION_STRING_ENCODING, "UTF-8"));

  /*
   * Event ID's
   */
  int MPV_EVENT_START_FILE = 6;   // Notification before playback start of a file (before the file is loaded).
  int MPV_EVENT_END_FILE = 7;     // Notification after playback end (after the file was unloaded).
  int MPV_EVENT_FILE_LOADED = 8;  // Notification when the file has been loaded (headers were read etc.), and decoding starts.


  /*
   * Error codes
   */

  /**
   * The accessed property doesn't exist.
   */
  int MPV_ERROR_PROPERTY_NOT_FOUND = -8;

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

  long mpv_client_api_version();
  long mpv_create();
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