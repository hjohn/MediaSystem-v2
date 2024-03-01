package hs.mediasystem.db.extract;

public record StreamMetaDataRecord(int contentId, long lastModificationTime, int version, byte[] json) {}
