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
  int MPV_EVENT_IDLE = 11;
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

  mpv_event mpv_wait_event(long handle, double timeOut);
  int mpv_request_event(long handle, int event_id, int enable);

  class mpv_event extends Structure {
    public int event_id;
    public int error;
    public long reply_userdata;
    public Pointer data;

    @Override
    protected List<String> getFieldOrder() {
      return List.of("event_id", "error", "reply_userdata", "data");
    }
  }
}