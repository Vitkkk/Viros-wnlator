package com.winlator.library.launch;

import androidx.appcompat.app.AppCompatActivity;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.library.storage.LibraryPreferences;
import com.winlator.xenvironment.RootFS;
import com.winlator.xenvironment.RootFSInstaller;

import org.json.JSONException;
import org.json.JSONObject;

public class AutomaticContainerProvider {
    private static final String AUTO_CONTAINER_NAME = "Viros Games";

    public interface Callback {
        void onReady(Container container);
        void onPreparingSystem();
        void onError(String message);
    }

    public void ensure(AppCompatActivity activity, Callback callback) {
        RootFS rootFS = RootFS.find(activity);
        if (!rootFS.isValid() || rootFS.getVersion() < RootFSInstaller.LATEST_VERSION) {
            RootFSInstaller.installIfNeeded(activity);
            callback.onPreparingSystem();
            return;
        }

        ContainerManager manager = new ContainerManager(activity);
        LibraryPreferences preferences = new LibraryPreferences(activity);
        int preferredId = preferences.getAutoContainerId();
        if (preferredId > 0) {
            Container container = manager.getContainerById(preferredId);
            if (container != null) {
                callback.onReady(container);
                return;
            }
        }

        for (Container container : manager.getContainers()) {
            if (AUTO_CONTAINER_NAME.equals(container.getName())) {
                preferences.setAutoContainerId(container.id);
                callback.onReady(container);
                return;
            }
        }

        try {
            JSONObject data = new JSONObject();
            data.put("name", AUTO_CONTAINER_NAME);
            manager.createContainerAsync(data, container -> {
                if (container == null) {
                    callback.onError("Não foi possível criar o ambiente automático do jogo.");
                    return;
                }
                preferences.setAutoContainerId(container.id);
                callback.onReady(container);
            });
        }
        catch (JSONException e) {
            callback.onError("Não foi possível preparar a configuração do jogo.");
        }
    }
}
