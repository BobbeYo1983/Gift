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
         * Инициализация объекта класса для работы с покупками, делал по https://habr.com/ru/post/444072/.
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

                            //TODO A successful purchase also generates a purchase token, which is a unique identifier that represents
                            // the user and the product ID for the in-app product they purchased. Your apps can store the purchase
                            // token locally, though we recommend passing the token to your secure backend server where you can then
                            // verify the purchase and protect against fraud.

                            ReturnTheGoods(); //покупка выполнена, отдаем пользователю товар

                        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {

                            Log.v(TAG_LOG, "Пользователь отменил покупку, нужно проверить, что товар не дотупен для пользователя");

                        } else {

                            Log.v(TAG_LOG, "Неизвестная ошибка покупки товара. Нужно сохранить информацию на сервер");

                        }
                    }
                })
                .enablePendingPurchases()
                .build();



        ConnectionToService(); //подключение к сервису покупок



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

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {

                //если подключение выполнено успешно
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                    Log.v(TAG_LOG, "Подключение к сервису покупок выполнено успешно");

                    QuerySkuDetails(); //запрос информации о товарах

                    // TODO показать товар пользователю

                    // TODO если список с покупками не пустой, то последовательно запускать покупку каждого товара
                    // перенести этот код в проверку покупок
                    List<Purchase> purchasesList = QueryPurchases(); //запрос о покупках когда подключились
                    //если товар уже куплен, предоставить его пользователю
                    for (int i = 0; i < purchasesList.size(); i++) {
                        String purchaseId = purchasesList.get(i).getSku();
                        //Если в списке покупок есть товар, то вернуть его пользователю
                        if(TextUtils.equals(skuId, purchaseId)) {
                            ReturnTheGoods();
                        }
                    }

                }
            }

            @Override
            public void onBillingServiceDisconnected() {

                Log.v(TAG_LOG, "Ошибка подключения к сервису покупок, нужно реализовать повтор");


                // TODO You must also implement retry logic to handle lost connections to Google Play.
                // To implement retry logic, override the onBillingServiceDisconnected() callback method,
                // and make sure that the BillingClient calls the startConnection() method to reconnect to
                // Google Play before making further requests.

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
                    for (SkuDetails skuDetails : list) {
                        mapSkuDetails.put(skuDetails.getSku(), skuDetails); // получаем карту с товарами
                    }
                }
            }
        });
    }

    /**
     * Запуск процедуры покупки
     * @param skuId идентификатор товара
     */
    public void LaunchBilling(String skuId) {

        Log.v(TAG_LOG, "Запуск процедуры покупки");

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(mapSkuDetails.get(skuId))
                .build();

        // TODO The launchBillingFlow() method returns one of several response codes listed in BillingClient.BillingResponseCode.
        // Be sure to check this result to ensure there were no errors launching the purchase flow.
        // A BillingResponseCode of OK indicates a successful launch.
        // Обработать код ошибки
        // если успешно должно появиться окошко на активити
        // если пользователь купит, то дальше в onPurchasesUpdated
        int responseCode = billingClient.launchBillingFlow(this, billingFlowParams).getResponseCode();
    }



    /**
     * Выполняет процедуру отдачи товара пользователю после покупки.
     */
    private void ReturnTheGoods() {

        Log.v(TAG_LOG, "Отдаем пользователю купленный товар");

    }


    /**
     * Запрос покупок, сделанных пользователем.
     * @return список всех покупок
     */
    private List<Purchase> QueryPurchases() {

        Log.v(TAG_LOG, "Запрос покупок, сделанных пользователем");

        //TODO To handle these situations, be sure that your app calls BillingClient.queryPurchases()
        // in your onResume() and onCreate() methods to ensure that all purchases are successfully
        // processed as described in processing purchases.
        // выполнить Handling purchases made outside your app
        // выполнить Handling pending transactions

        //TODO Делаем проверку: если товар куплен — выполнить payComplete().
        // Выполнить обработку ПОКУПОК https://developer.android.com/google/play/billing/integrate#java

        Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
        return purchasesResult.getPurchasesList();
    }
}