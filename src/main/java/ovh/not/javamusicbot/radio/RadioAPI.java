package ovh.not.javamusicbot.radio;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import ovh.not.javamusicbot.utils.CachedObject;

public class RadioAPI {
    
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
    
    public void getRadios(Consumer<List<Radio>> radiosConsumer) {
        this.radioList.getAsync(radiosConsumer);
    }
    
    private void getRadios0(Consumer<List<Radio>> radiosConsumer) {
        Request request = new Request.Builder()
                                     .url(GET_RADIOS)
                                     .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // TODO: handle properly
                radiosConsumer.accept(Collections.emptyList());
            }
    
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
    
                    // TODO: parse radios
                }
            }
        });
    }
    
    public void getRadio(String name, Consumer<Radio>radioConsumer) {
        String lowerCaseName = name.toLowerCase();
        this.radioByName.computeIfAbsent(lowerCaseName, $ -> new CachedObject<>(this.getRadioConsumer(lowerCaseName), CACHE_TIMEOUT))
                        .getAsync(radioConsumer);
    }
    
    public Consumer<Consumer<Radio>> getRadioConsumer(String name) {
        return radioConsumer -> {
            Request request = new Request.Builder()
                                         .url(GET_RADIO_BY_NAME + name)
                                         .build();
            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    // TODO: handle properly
                    radioConsumer.accept(null);
                }
        
                @Override public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        
                        // TODO: parse radio
                        // TODO: pass radio
                        radioConsumer.accept(new Radio());
                    }
                }
            });
        };
    }
 }