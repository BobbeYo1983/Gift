package ru.zizi.gift;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private BillingClient billingClient; // объект клиента для работы с оплатой
    private final String skuId = "sku_id_1"; // id-товара
    private final Map<String, SkuDetails> mapSkuDetails = new HashMap<>(); //список всех товаров
    private final String TAG_LOG = "!@#";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //ищем вьюхи
        Button buttonBuy = findViewById(R.id.buttonBuy); //кнопка покупки товара



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

                            Log.v(TAG_LOG, "Выполнена покупка");

                            ReturnTheGoods(list); //покупка выполнена, отдаем пользователю товары

                        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {

                            Log.v(TAG_LOG, "Пользователь отменил покупку, нужно проверить, что товар не дотупен для пользователя");

                        } else {

                            Log.v(TAG_LOG, "Неизвестная ошибка покупки товара. Нужно сохранить информацию на сервер");

                        }
                    }
                })
                .enablePendingPurchases() // включить отложенные покупки
                .build();



        //ConnectionToService(); //подключение к сервису покупок



        //привязываем выполнение покупки на кнопку
        buttonBuy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LaunchBilling(skuId); // запуск процедуры покупки
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
        skuList.add(skuId); // запрашиваем об этом товаре
        skuDetailsParamsBuilder.setSkusList(skuList).setType(BillingClient.SkuType.INAPP); // INAPP для одноразовых покупок
        billingClient.querySkuDetailsAsync(skuDetailsParamsBuilder.build(), new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(@NonNull BillingResult billingResult, @Nullable List<SkuDetails> list) {
                if (billingResult.getResponseCode() == 0) {
                    if (list.size() > 0) {
                        for (SkuDetails skuDetails : list) {

                            mapSkuDetails.put(skuDetails.getSku(), skuDetails); // получаем карту с товарами
                            Log.v(TAG_LOG, "С сервера получена информация о товаре = " + skuDetails.getSku());

                        }

                        // TODO показать товары

                    } else {
                        Log.v(TAG_LOG, "Список товаров полученный с сервера пуст");
                    }
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
                Log.v(TAG_LOG, "Процедура закупки началась успешно");
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

        Log.v(TAG_LOG, "Отдаем пользователю купленный товар");

        for (Purchase purchase : purchasesList) {

            //если статус покупки завершенный. Бывает еще статус отложенной покупки, когда пользователь начал покупать, но не завершил покупку
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {

                Log.v(TAG_LOG, "Статус покупки товара с Id = " + purchase.getSku() + " завершенный");

                Log.v(TAG_LOG, "token покупки = " + purchase.getPurchaseToken());

                // TODO нужно проверять была ли раньше покупка с таким же токеном, а для этого хранить в бэкенде

                //TODO отдать товар

                //TODO При желании отметьте товар как использованный, чтобы пользователь мог купить его снова.

            } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {

                Log.v(TAG_LOG, "Статус покупки товара с Id = " + purchase.getSku() + " отложенный");
            }


        }

    }



    /**
     * Запрос покупок, сделанных пользователем.
     */
    private void QueryPurchases() {

        Log.v(TAG_LOG, "Запрос покупок, сделанных пользователем");

        Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP); // запрос одноразовых товаров
        List<Purchase> purchasesList = purchasesResult.getPurchasesList();

        if (purchasesList.size() > 0) {
            Log.v(TAG_LOG, "У пользователя есть покупки");

            for (Purchase purchase : purchasesList) {

                Log.v(TAG_LOG, "Нужно вернуть товар с Id = " + purchase.getSku());
            }

            ReturnTheGoods(purchasesList); //отдать товар пользователю по списку

        }

    }



    @Override
    protected void onResume() {
        super.onResume();

        ConnectionToService();
    }
}