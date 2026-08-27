package com.winlator.library.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "games",
    indices = {
        @Index(value = {"executableUri"}, unique = true),
        @Index(value = {"resolvedLocalPath"})
    }
)
public class GameEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public String folderUri;
    public String executableUri;
    public String executableRelativePath;
    public String resolvedLocalPath;
    public String customCoverUri;
    public String detectedEngine;
    public int containerId;
    public String performanceMode;
    public Integer inputProfileId;
    public boolean favorite;
    public long lastPlayed;
    public long totalPlayTimeMs;
    public String customArguments;
    public String environmentVariables;
    public long sourceLastModified;
    public long sourceSize;

    public GameEntity() {
        performanceMode = "automatic";
        detectedEngine = "generic";
        customArguments = "";
        environmentVariables = "";
    }
}
