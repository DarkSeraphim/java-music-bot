package ovh.not.javamusicbot.radio;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonIOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import ovh.not.javamusicbot.MusicBot;
import ovh.not.javamusicbot.utils.CachedObject;

import javax.annotation.ParametersAreNonnullByDefault;

public class RadioAPI {

    private static class RadioList {
        private final Radio[] results = null;

        private final int total = -1;

        boolean isValid() {
            return total >= 0 && results != null;
        }
    }

    private static final String BASE_URL = "https://takeoff.dabbot.org/radio-api";
    
    private static final String GET_RADIOS = BASE_URL + "/radios";
    
    private static final String GET_RADIO_BY_NAME = BASE_URL + "/radios/";
    
    private static final long CACHE_TIMEOUT = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
    
    private final OkHttpClient client;
    
    private final CachedObject<List<Radio>> radioList;
    
    private final Map<String, CachedObject<Radio>> radioByName = new ConcurrentHashMap<>();
    
    public RadioAPI(OkHttpClient client) {
        this.client = client;
        this.radioList = new CachedObject<>(this::getRadios0, CACHE_TIMEOUT);
    }
    
    public void getRadios(BiConsumer<List<Radio>, Throwable> radiosConsumer) {
        this.radioList.getAsync(radiosConsumer);
    }
    
    private void getRadios0(BiConsumer<List<Radio>, Throwable> radiosConsumer) {
        Request request = new Request.Builder()
                                     .url(GET_RADIOS)
                                     .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                radiosConsumer.accept(null, e);
            }
    
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        throw new IOException("Unexpected null body");
                    }

                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }

                    RadioList list = MusicBot.GSON.fromJson(responseBody.charStream(), RadioList.class);
                    if (list == null || !list.isValid()) {
                        throw new IOException("Malformed JSON response");
                    }
                    radiosConsumer.accept(Arrays.asList(list.results), null);
                }
            }
        });
    }
    
    public void getRadio(String name, BiConsumer<Radio, Throwable> radioConsumer) {
        if (name == null) {
            throw new NullPointerException("Name is null");
        }
        String lowerCaseName = name.toLowerCase();
        if (this.radioList.hasValue()) {
            for (Radio radio : this.radioList.getUncached()) {
                if (lowerCaseName.equals(radio.getName())) {
                    radioConsumer.accept(radio, null);
                    return;
                }
            }
        }
        this.radioByName.computeIfAbsent(lowerCaseName, $ -> new CachedObject<>(this.getRadioConsumer(lowerCaseName), CACHE_TIMEOUT))
                        .getAsync(radioConsumer);
    }
    
    private Consumer<BiConsumer<Radio, Throwable>> getRadioConsumer(String name) {
        return radioConsumer -> {
            Request request = new Request.Builder()
                                         .url(GET_RADIO_BY_NAME + name)
                                         .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    radioConsumer.accept(null, e);
                }
        
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody == null) {
                            throw new IOException("Unexpected null body");
                        }

                        if (!response.isSuccessful()) {
                            throw new IOException("Unexpected code " + response);
                        }
        
                        Radio radio = MusicBot.GSON.fromJson(responseBody.charStream(), Radio.class);
                        if (radio == null) {
                            throw new IOException("Malformed JSON response");
                        }
                        radioConsumer.accept(radio, null);
                    }
                }
            });
        };
    }
 }