package ru.zizi.gift;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private BillingClient billingClient; // объект клиента для работы с оплатой
    private final String skuIdTest = "test"; // id-товара
    private final String skuIdChocolate = "chocolate"; // id-товара
    private final String skuIdChampagne = "champagne"; // id-товара
    private final String skuIdBouquet = "bouquet"; // id-товара
    private final Map<String, SkuDetails> mapSkuDetails = new HashMap<>(); //список всех товаров
    private final String TAG_LOG = "!@#";
    private String purchaseToken = "empty";

    private FirebaseFirestore firebaseFirestore;
    //private FirebaseAuth firebaseAuth; // объект для работы с авторизацией в Firebase

    private RadioButton radioButton1;
    private RadioButton radioButton2;
    private RadioButton radioButton3;
    private TextInputEditText aboutYou;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //FirebaseAuth firebaseAuth = FirebaseAuth.getInstance(); // инициализация объект для работы с авторизацией в FireBase
        //firebaseAuth.signInAnonymously();
        firebaseFirestore = FirebaseFirestore.getInstance(); // инициализация объект для работы с базой

        // узнаем разрешение экрана
        Display display = getWindowManager().getDefaultDisplay();
        Point displaySize = new Point();
        display.getSize(displaySize);
        int x = Math.round(displaySize.x/2)-1;
        int y = (int) (x * 1.777); // соотношение 16:9


        //ищем вьюхи
        Button buttonBuy = findViewById(R.id.buttonBuy); //кнопка покупки товара
        aboutYou = findViewById(R.id.aboutYou);
        radioButton1 = findViewById(R.id.radioButton1);
        radioButton2 = findViewById(R.id.radioButton2);
        radioButton3 = findViewById(R.id.radioButton3);
        ImageView imageView1 = findViewById(R.id.imageView1);
        ImageView imageView2 = findViewById(R.id.imageView2);
        ImageView imageView3 = findViewById(R.id.imageView3);
        ImageView imageView4 = findViewById(R.id.imageView4);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(x,y);
        imageView1.setLayoutParams(layoutParams);
        imageView2.setLayoutParams(layoutParams);
        imageView3.setLayoutParams(layoutParams);
        imageView4.setLayoutParams(layoutParams);


       // GridView gridview = (GridView) findViewById(R.id.gridView);
       //gridview.setAdapter(new ImageAdapter(this, displaySize));

        /**
         * Инициализация объекта класса для работы с покупками
         * Слушатель, когда покупка будет выполнена
         */
        billingClient = BillingClient
                .newBuilder(this)
                .setListener(new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {

                        //если результат покупки успешный и список с покупками не пустой
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {

                            Log.v(TAG_LOG, "Выполнена покупка, нужно проверить статус покупок, завершенные или отложенные");

                            CheckStatusPurchases(list); //покупка выполнена, нужно проверить статус покупок, завершенные или отложенные

                        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {

                            Log.v(TAG_LOG, "Пользователь отменил покупку, нужно проверить, что товар не дотупен для пользователя");

                        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {

                            Log.v(TAG_LOG, "Товар уже куплен, нужно показать это пользователю, если он хочет его купить еще раз.ю то попытаться это сделать через 5 минут");

                        } else {

                            Log.v(TAG_LOG, "Неизвестная ошибка покупки товара. Код ошибки: " + billingResult.getResponseCode());

                        }
                    }
                })
                .enablePendingPurchases() // включить отложенные покупки
                .build();



        //привязываем выполнение покупки на кнопку
        buttonBuy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.v(TAG_LOG, "Нажата кнопка КУПИТЬ И ОТПРАВИТЬ СООБЩЕНИЕ");

                if (!aboutYou.getText().toString().isEmpty()) { // если поле о себе заполнено

                    //LaunchBilling(skuIdTest); // запуск процедуры покупки

                    if (radioButton1.isChecked()) {
                        LaunchBilling(skuIdChocolate);
                    }

                    if (radioButton2.isChecked()) {
                        LaunchBilling(skuIdChampagne);
                    }

                    if (radioButton3.isChecked()) {
                        LaunchBilling(skuIdBouquet);
                    }
                } else {

                    Toast.makeText(getApplicationContext(), "Напиши о себе", Toast.LENGTH_LONG).show();
                }

            }
        });

    }



    /**
     * Сохранение сообщения в БД
     */
    private void SaveMessage(){

        Log.v(TAG_LOG, "Сохраняем сообщение в БД");

        Map<String, Object> data = new HashMap<>();
        data.put("message", aboutYou.getText().toString());

        Task<DocumentReference> documentReference = firebaseFirestore.collection("messages")
                .add(data).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        Log.v(TAG_LOG, "Задача по сохраненияю в БД выполнена");
                        if (task.isSuccessful()) { //если сохранение успешно
                            Log.v(TAG_LOG, "Сообщение сохранено в БД");
                        }else {
                            Log.v(TAG_LOG, "Ошибка сохранения в БД: " + task.getException().getMessage());
                        }

                    }
                });

    }



    /**
     * Подключение к сервису покупок
     */
    private void ConnectionToService () {

        Log.v(TAG_LOG, "Подключение к сервису покупок");

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {

                //если подключение выполнено успешно
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                    Log.v(TAG_LOG, "Подключение к сервису покупок выполнено успешно");

                    QuerySkuDetails(); //запрос информации о товарах

                    QueryPurchases(); //запрос о покупках когда подключились

                }
            }

            @Override
            public void onBillingServiceDisconnected() {

                Log.v(TAG_LOG, "Ошибка подключения к сервису покупок, нужно реализовать повтор");

                // TODO наверное нужно сделать несколько попыток и потом сказать попробуйте позже
                ConnectionToService();

            }
        });
    }



    /**
     * Запрос информации о товарах с сервера
     */
    private void QuerySkuDetails() {

        Log.v(TAG_LOG, "Запрос информации о товарах с сервера");

        SkuDetailsParams.Builder skuDetailsParamsBuilder = SkuDetailsParams.newBuilder();
        List<String> skuList = new ArrayList<>();
        //skuList.add(skuId); // запрашиваем об этом товаре
        skuList.add(skuIdTest); // запрашиваем об этом товаре
        skuList.add(skuIdChocolate); // запрашиваем об этом товаре
        skuList.add(skuIdChampagne); // запрашиваем об этом товаре
        skuList.add(skuIdBouquet); // запрашиваем об этом товаре
        skuDetailsParamsBuilder.setSkusList(skuList).setType(BillingClient.SkuType.INAPP); // INAPP для одноразовых покупок
        billingClient.querySkuDetailsAsync(skuDetailsParamsBuilder.build(), new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(@NonNull BillingResult billingResult, @Nullable List<SkuDetails> listSkuDetails) {

                Log.v(TAG_LOG, "Статус результата запроса информации о товарах с сервера = " + billingResult.getResponseCode() + " Размер списка товаров = " + listSkuDetails.size());

                if (billingResult.getResponseCode() == 0) {
                    if (listSkuDetails.size() > 0) {
                        for (SkuDetails skuDetails : listSkuDetails) {

                            mapSkuDetails.put(skuDetails.getSku(), skuDetails); // получаем карту с товарами
                            Log.v(TAG_LOG, "С сервера получена информация о товаре = " + skuDetails.getSku());

                        }

                        // TODO Показываем информацию от товарах

                    } else {
                        Log.v(TAG_LOG, "Список товаров полученный с сервера пуст");
                    }
                } else {
                    Log.v(TAG_LOG, "Ошибка при запросе информации о товарах с сервера. Код ошибки: " + billingResult.getResponseCode());
                }

            }
        });
    }



    /**
     * Запуск процедуры покупки одного товара
     * @param skuId идентификатор товара
     */
    public void LaunchBilling(String skuId) {

        Log.v(TAG_LOG, "Запуск процедуры покупки");

        //проверяем есть ли такой товар на сервере
        if (mapSkuDetails.get(skuId) != null) {

            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(mapSkuDetails.get(skuId))
                    .build();

            int responseCode = billingClient.launchBillingFlow(this, billingFlowParams).getResponseCode(); // получаем статус начала процедуры закупки

            if (responseCode == BillingClient.BillingResponseCode.OK) {
                Log.v(TAG_LOG, "Процедура закупки началась успешно. Пользователь открыл окошко с кнопкой КУПИТЬ.");
            } else {
                Log.v(TAG_LOG, "Процедура закупки началась не успешно. Код ошибки = " + responseCode);
            }

        } else {

            Log.v(TAG_LOG, "Закупаемого товара нет в списке с сервера. Либо список пуст и не запрашивался с сервера, либо нужно создать нужный товар на сервере");

        }

    }



    /**
     * Выполняет процедуру отдачи товара пользователю после покупки.
     * @param purchasesList список покупок
     */
    private void ReturnTheGoods(List<Purchase> purchasesList) {

        for (Purchase purchase : purchasesList) {

            Log.v(TAG_LOG, "Проверим ранее покупался ли товар с Id = " + purchase.getSku() + " c токеном покупки = " + purchase.getPurchaseToken());
            // нужно проверять была ли раньше покупка с таким же токеном, а для этого хранить в бэкенде
            if (!purchaseToken.equals(purchase.getPurchaseToken())) { //если новый токен, то сохраняем в БД

                //TODO отдать товар
                Log.v(TAG_LOG, "Отдаем купленный товар с Id = " + purchase.getSku());
                SaveMessage();

                //TODO При желании отметьте товар как использованный, чтобы пользователь мог купить его снова.

                purchaseToken = purchase.getPurchaseToken(); //запоминаем токен

            }


        }

    }



    /**
     * Запрос покупок, сделанных пользователем.
     */
    private void QueryPurchases() {

        Log.v(TAG_LOG, "Проверка, есть ли у пользователя покупки");

        Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP); // запрос одноразовых товаров
        List<Purchase> purchasesList = purchasesResult.getPurchasesList();

        if (purchasesList.size() > 0) {
            Log.v(TAG_LOG, "У пользователя ЕСТЬ какие-то покупки. Нужно проверить завершенные или отложенные.");
            CheckStatusPurchases(purchasesList);
        } else {
            Log.v(TAG_LOG, "У пользователя НЕТ покупок");
        }


    }


    /**
     * Проверка статуса покупок из списка. Статус покупки может быть завершенной или отложенной.
     * Если покупка отложенная, вычеркиваем ее из списка на выдачу товара пользователю.
     * @param purchasesList список покупок
     */
    private void CheckStatusPurchases (List<Purchase> purchasesList){

        Log.v(TAG_LOG, "Проверка статуса покупок по списку. Отложенные или завершенные. ");

        List<Purchase> purchasesListDelivery = new ArrayList<Purchase>(); //Список товаров на выдачу пользователю

        for (Purchase purchase : purchasesList) {

            //если статус покупки завершенный. Бывает еще статус отложенной покупки, когда пользователь начал покупать, но не завершил покупку
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {

                Log.v(TAG_LOG, "Статус покупки товара с Id = " + purchase.getSku() + " завершенный, товар ДОБАВЛЯЕМ в список на выдачу");
                purchasesListDelivery.add(purchase);

            } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {

                Log.v(TAG_LOG, "Статус покупки товара с Id = " + purchase.getSku() + " отложенный, товар НЕ ДОБАВЛЯЕМ в список на выдачу");

            } else {

                Log.v(TAG_LOG, "Статус покупки товара с Id = " + purchase.getSku() + " неизвестный");
            }

        }

        //Если список на выдачу товара не пустой, то отдадим товар
        if (purchasesListDelivery.size() > 0) {
            Log.v(TAG_LOG, "Есть завершенные покупки, можно выдать товар");
            ReturnTheGoods(purchasesListDelivery); //отдать товар пользователю по списку
        }
    }



    @Override
    protected void onResume() {
        super.onResume();

        ConnectionToService();
    }

/*    private void UpdateUI(List<SkuDetails> listSkuDetails) {

        RadioButton radioButton1 = findViewById(R.id.radioButton1);
        RadioButton radioButton2 = findViewById(R.id.radioButton2);
        RadioButton radioButton3 = findViewById(R.id.radioButton3);

        for (SkuDetails skuDetails : listSkuDetails) {
            if (skuDetails.getSku().equals("chocolate")) {
                radioButton1.setText("Вкусная шоколадка,");
            }
        }


    }*/
}