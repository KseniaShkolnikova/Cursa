package com.example.ozon;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.search.SearchFactory;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SearchManagerType;
import com.yandex.mapkit.search.SearchOptions;
import com.yandex.mapkit.search.Session;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;
import com.yandex.runtime.network.NetworkError;
import com.yandex.runtime.network.RemoteError;

/**
 * Класс MapSelectionActivity представляет собой активность для выбора адреса доставки
 * на карте в приложении "OZON". Использует Yandex MapKit для отображения
 * карты, определения текущего местоположения пользователя и поиска адреса по координатам.
 * Позволяет пользователю вручную выбрать точку на карте или использовать текущее местоположение.
 */
public class MapSelectionActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String YANDEX_MAPKIT_API_KEY = "3847ea55-35fb-4a64-a196-4839fac767be";
    private static boolean isMapKitInitialized = false;
    private MapView mapView;
    private SearchManager searchManager;
    private Point selectedPoint;
    private String selectedAddress;
    private Session searchSession;
    private Button btnConfirm;
    private Button btnResetSelection;
    private Button btnCancelSelection;
    private InputListener mapInputListener;
    private boolean isManualSelection = false;
    private boolean isInitialAddressShown = false;
    private PlacemarkMapObject userLocationMarker;
    private PlacemarkMapObject selectedLocationMarker;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    /**
     * Инициализирует активность выбора адреса на карте. Настраивает Yandex MapKit, проверяет
     * наличие интернет-соединения, инициализирует элементы UI, карту и сервисы определения
     * местоположения. Запрашивает разрешения на доступ к местоположению и уведомлениям.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        synchronized (MapSelectionActivity.class) {
            if (!isMapKitInitialized) {
                MapKitFactory.setApiKey(YANDEX_MAPKIT_API_KEY);
                MapKitFactory.initialize(this);
                isMapKitInitialized = true;
            }
        }
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Нет интернет-соединения", Toast.LENGTH_LONG).show();
        }
        setContentView(R.layout.activity_map);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
        initViews();
        setupMap();
        initLocationServices();
        checkLocationPermissionAndMoveToCurrentLocation();
    }

    /**
     * Проверяет наличие интернет-соединения. Использует ConnectivityManager для определения
     * состояния сети, учитывая различия в API для разных версий Android.
     */
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return cm.getActiveNetwork() != null;
            } else {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Инициализирует элементы UI, такие как карта, кнопки подтверждения, сброса и отмены выбора.
     * Настраивает обработчики событий для кнопок.
     */
    private void initViews() {
        mapView = findViewById(R.id.mapview);
        btnConfirm = findViewById(R.id.btnConfirmSelection);
        btnResetSelection = findViewById(R.id.btnResetLocation);
        btnCancelSelection = findViewById(R.id.btnCancelSelection);
        btnConfirm.setEnabled(false);
        btnResetSelection.setVisibility(View.GONE);
        btnConfirm.setOnClickListener(v -> {
            if (selectedPoint != null && selectedAddress != null && !selectedAddress.equals("Определение адреса...")) {
                returnResult();
            } else {
                Toast.makeText(this, "Дождитесь определения адреса", Toast.LENGTH_SHORT).show();
            }
        });
        btnResetSelection.setOnClickListener(v -> {
            isManualSelection = false;
            if (selectedLocationMarker != null) {
                mapView.getMap().getMapObjects().remove(selectedLocationMarker);
                selectedLocationMarker = null;
            }
            btnResetSelection.setVisibility(View.GONE);
            resetToLocationTracking();
        });
        btnCancelSelection.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            setResult(RESULT_CANCELED, resultIntent);
            finish();
        });
    }

    /**
     * Инициализирует сервисы определения местоположения с использованием FusedLocationProviderClient.
     * Подготавливает объект для получения обновлений местоположения.
     */
    private void initLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationRequest();
    }

    /**
     * Создает запрос на обновление местоположения с заданными параметрами (интервал, приоритет).
     * Настраивает callback для обработки новых данных о местоположении.
     */
    private void createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || isManualSelection) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    updateLocationOnMap(location);
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    /**
     * Обновляет отображение текущего местоположения пользователя на карте. Добавляет маркер
     * текущей позиции и перемещает камеру карты к этой точке, если не выбран ручной режим.
     */
    private void updateLocationOnMap(Location location) {
        if (mapView != null && location != null) {
            Point currentLocation = new Point(location.getLatitude(), location.getLongitude());
            if (userLocationMarker != null) {
                mapView.getMap().getMapObjects().remove(userLocationMarker);
            }
            userLocationMarker = mapView.getMap().getMapObjects().addPlacemark(currentLocation);
            userLocationMarker.setIcon(ImageProvider.fromResource(this, R.drawable.myaddres));
            if (!isManualSelection) {
                mapView.getMap().move(
                        new CameraPosition(currentLocation, 15.0f, 0.0f, 0.0f),
                        new Animation(Animation.Type.SMOOTH, 1),
                        null
                );
                selectedPoint = currentLocation;
                if (!isInitialAddressShown) {
                    searchAddress(currentLocation);
                }
            }
        }
    }

    /**
     * Настраивает карту Yandex MapKit. Устанавливает начальную позицию камеры, инициализирует
     * менеджер поиска адресов и добавляет слушатель для обработки касаний карты.
     */
    private void setupMap() {
        Point startPoint = new Point(55.751574, 37.573856);
        mapView.getMap().move(new CameraPosition(startPoint, 15f, 0f, 0f));
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED);
        if (mapInputListener != null) {
            mapView.getMap().removeInputListener(mapInputListener);
        }
        mapInputListener = new InputListener() {
            @Override
            public void onMapTap(@NonNull Map map, @NonNull Point point) {
                handleMapTap(point);
            }
            @Override
            public void onMapLongTap(@NonNull Map map, @NonNull Point point) {
            }
        };

        mapView.getMap().addInputListener(mapInputListener);
    }

    /**
     * Обрабатывает касание карты пользователем. Переключает режим на ручной выбор, добавляет
     * маркер выбранной точки, перемещает камеру к этой точке и инициирует поиск адреса.
     */
    private void handleMapTap(Point point) {
        isManualSelection = true;
        btnResetSelection.setVisibility(View.VISIBLE);
        if (selectedLocationMarker != null) {
            mapView.getMap().getMapObjects().remove(selectedLocationMarker);
        }
        selectedLocationMarker = mapView.getMap().getMapObjects().addPlacemark(point);
        selectedLocationMarker.setIcon(ImageProvider.fromResource(this, R.drawable.selectaddres));
        mapView.getMap().move(
                new CameraPosition(point, 15f, 0f, 0f),
                new Animation(Animation.Type.SMOOTH, 0.3f),
                null
        );
        selectedPoint = point;
        selectedAddress = "Определение адреса...";
        btnConfirm.setEnabled(false);
        if (searchSession != null) {
            searchSession.cancel();
        }
        searchAddress(point);
    }

    /**
     * Сбрасывает выбор адреса к текущему местоположению пользователя. Удаляет маркер ручного
     * выбора и запрашивает последнее известное местоположение для обновления карты.
     */
    private void resetToLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            updateLocationOnMap(location);
                        }
                    });
        }
    }

    /**
     * Выполняет поиск адреса по заданным координатам с использованием Yandex MapKit Search.
     * Обновляет выбранный адрес и активирует кнопку подтверждения при успешном поиске.
     */
    private void searchAddress(Point point) {
        searchSession = searchManager.submit(
                point,
                15,
                new SearchOptions(),
                new Session.SearchListener() {
                    @Override
                    public void onSearchResponse(@NonNull com.yandex.mapkit.search.Response response) {
                        try {
                            if (response.getCollection() != null && !response.getCollection().getChildren().isEmpty()) {
                                selectedAddress = response.getCollection().getChildren().get(0).getObj().getName();
                            } else {
                                selectedAddress = "Адрес не определен";
                            }
                            runOnUiThread(() -> {
                                if (!isInitialAddressShown || isManualSelection) {
                                    Toast.makeText(MapSelectionActivity.this,
                                            "Выбрано: " + selectedAddress,
                                            Toast.LENGTH_LONG).show();
                                    isInitialAddressShown = true;
                                }
                                btnConfirm.setEnabled(true);
                            });
                        } catch (Exception e) {
                        }
                    }
                    @Override
                    public void onSearchError(@NonNull Error error) {
                        runOnUiThread(() -> {
                            String errorMessage = "Ошибка при поиске адреса";
                            if (error instanceof NetworkError) {
                                errorMessage = "Нет интернет-соединения";
                            } else if (error instanceof RemoteError) {
                                errorMessage = "Ошибка сервера";
                            }
                            Toast.makeText(
                                    MapSelectionActivity.this,
                                    errorMessage,
                                    Toast.LENGTH_SHORT
                            ).show();
                            btnConfirm.setEnabled(false);
                            selectedAddress = "Ошибка определения адреса";
                        });
                    }
                }
        );
    }

    /**
     * Проверяет наличие разрешений на доступ к местоположению. Если разрешения предоставлены,
     * перемещает карту к текущему местоположению пользователя, иначе запрашивает разрешения.
     */
    private void checkLocationPermissionAndMoveToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            updateLocationOnMap(location);
                        } else {
                            Toast.makeText(this, "Не удалось определить местоположение", Toast.LENGTH_SHORT).show();
                            Point defaultPoint = new Point(55.751574, 37.573856);
                            mapView.getMap().move(new CameraPosition(defaultPoint, 15f, 0f, 0f));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка получения местоположения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Point defaultPoint = new Point(55.751574, 37.573856);
                        mapView.getMap().move(new CameraPosition(defaultPoint, 15f, 0f, 0f));
                    });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    /**
     * Обрабатывает результат запроса разрешений на доступ к местоположению. Если разрешения
     * предоставлены, перемещает карту к текущему местоположению, иначе отображает карту с
     * начальной точкой.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationPermissionAndMoveToCurrentLocation();
            } else {
                Toast.makeText(this,
                        "Для точного определения местоположения нужны разрешения",
                        Toast.LENGTH_LONG).show();
                Point startPoint = new Point(55.751574, 37.573856);
                mapView.getMap().move(new CameraPosition(startPoint, 15f, 0f, 0f));
            }
        }
    }

    /**
     * Возвращает результат выбора адреса (адрес, широту и долготу) в вызывающую активность
     * и завершает текущую активность.
     */
    private void returnResult() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("SELECTED_ADDRESS", selectedAddress);
        resultIntent.putExtra("LATITUDE", selectedPoint.getLatitude());
        resultIntent.putExtra("LONGITUDE", selectedPoint.getLongitude());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    /**
     * Вызывается при запуске активности. Активирует Yandex MapKit и MapView для отображения карты.
     */
    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
    }

    /**
     * Вызывается при остановке активности. Отменяет текущий поиск адреса, останавливает обновления
     * местоположения, удаляет слушатель карты и деактивирует Yandex MapKit и MapView.
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (searchSession != null) {
            searchSession.cancel();
        }
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (mapInputListener != null) {
            mapView.getMap().removeInputListener(mapInputListener);
        }
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
    }

    /**
     * Вызывается при приостановке активности. Останавливает обновления местоположения, чтобы
     * избежать лишних запросов в фоновом режиме.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    /**
     * Вызывается при возобновлении активности. Если разрешения на местоположение предоставлены,
     * возобновляет запросы на обновление местоположения.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            createLocationRequest();
        }
    }

    /**
     * Перехватывает события касания экрана. Используется для стандартной обработки событий касания.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }
}