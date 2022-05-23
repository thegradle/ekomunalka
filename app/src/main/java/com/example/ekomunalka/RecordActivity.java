package com.example.ekomunalka;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RecordActivity extends AppCompatActivity {

    DatabaseHelper db;
    MainActivity mainActivity;
    Spinner chooseMonth;
    Spinner chooseService;
    EditText currentReadings;
    Spinner chooseTariff;
    CheckBox isPaid;
    EditText commentText;
    TextView sum;
    Button saveData;
    String[] tariffs;

    Cursor receivedItem;
    Cursor tariffs_db;
    int id;

    ActivityResultLauncher<Intent> activityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();

                    if (data != null) {
                        if (data.getIntExtra("result", -1) == 1) {
                            tariffs_db = db.getTariffs();
                            refreshListOfTariffs();
                        }
                    }
                }
            });

    @SuppressLint("Range")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        setTitle("Редагувати запис");

        db = new DatabaseHelper(this);
        mainActivity = new MainActivity();
        currentReadings = findViewById(R.id.current);
        chooseService = findViewById(R.id.chooseService);
        ArrayAdapter<String> servicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mainActivity.services);
        chooseService.setAdapter(servicesAdapter);
        isPaid = findViewById(R.id.isPaid);
        saveData = findViewById(R.id.saveRecord);
        commentText = findViewById(R.id.comment);
        sum = findViewById(R.id.sum);
        chooseTariff = findViewById(R.id.chooseTariff);
        tariffs_db = db.getTariffs();
        tariffs = refreshListOfTariffs();

        Intent receivedIntent = getIntent();
        id = (int) receivedIntent.getLongExtra("id", -1);
        receivedItem = db.getRecord(id);
        tariffs_db = db.getTariffs();

        Map<String, String> data = GetDataFromDB();
        int chosenServiceId = GetServiceId(Objects.requireNonNull(data.get("service")));

        chooseMonth = findViewById(R.id.chooseMonth);
        ArrayAdapter<String> monthsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mainActivity.months);
        chooseMonth.setAdapter(monthsAdapter);
        chooseMonth.setSelection(Integer.parseInt(Objects.requireNonNull(data.get("date")).substring(0, Objects.requireNonNull(data.get("date")).length() - 5)));
        chooseService.setSelection(chosenServiceId);
        currentReadings.setText(data.get("current"));
        isPaid.setChecked(Objects.equals(data.get("paid"), "1"));
        commentText.setText(data.get("comment"));
        sum.setText("0 грн");

        currentValidate();
        sumCalculate();

        chooseMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentValidate();
                sumCalculate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        chooseService.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentValidate();
                sumCalculate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        chooseTariff.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == (tariffs.length - 1)) {
                    Intent intent = new Intent(RecordActivity.this, NewTariffActivity.class);
                    activityLauncher.launch(intent);
                } else {
                    currentValidate();
                    sumCalculate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        currentReadings.setOnClickListener(v -> {
            if (!currentValidate()) {
                mainActivity.Toast(RecordActivity.this,
                        "Спочатку оберіть сервіс, тариф та місяць!", true);
            }
        });

        currentReadings.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                sumCalculate();
            }
        });

        saveData.setOnClickListener(v -> {
            String date = Arrays.asList(mainActivity.months)
                    .indexOf(chooseMonth.getSelectedItem().toString()) + "." + Objects.requireNonNull(data.get("date")).substring(Objects.requireNonNull(data.get("date")).length() - 4);
            String service = chooseService.getSelectedItem().toString();
            String current = currentReadings.getText().toString();
            String paid = isPaid.isChecked() ? "1" : "0";
            String comment = commentText.getText().toString();
            String sum_result = sum.getText().toString();

            Map<String, String> newValues = GetDataFromLocal(date, service, current, paid, sum_result, comment);

            if (!data.equals(newValues)) {
                UpdateData(newValues, id);
            } else {
                if (Objects.requireNonNull(newValues.get("service")).isEmpty()) {
                    mainActivity.Toast(this, "Введіть значення!", true);
                } else {
                    mainActivity.Toast(this, "Дані ті самі!", true);
                }
            }
        });
    }

    public void UpdateData(Map<String, String> newValues, int id) {
        boolean insertData = db.updateRecord(newValues, id);

        if (insertData) {
            mainActivity.Toast(this, "Дані оновлено!", false);
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("result", 1);
            setResult(RESULT_OK, intent);
            RecordActivity.super.onBackPressed();
        } else {
            mainActivity.Toast(this, "Щось пішло не так...", true);
        }
    }

    public Map<String, String> GetDataFromDB() {
        Map<String, String> data = new HashMap<>();

        while (receivedItem.moveToNext()) {
            data.put("date", receivedItem.getString(1));
            data.put("service", receivedItem.getString(2));
            data.put("current", receivedItem.getString(3));
            data.put("paid", receivedItem.getString(4));
            data.put("comment", receivedItem.getString(5));
        }

        return data;
    }

    public Map<String, String> GetDataFromLocal(String date, String service, String current, String paid, String sum, String comment) {
        Map<String, String> data = new HashMap<>();

        data.put("date", date);
        data.put("service", service);
        data.put("current", current);
        data.put("paid", paid);
        data.put("sum", sum);
        data.put("comment", comment);

        return data;
    }

    public int GetServiceId(String service) {
        int id = -1;

        switch (service) {
            case "Вода":
                id = 1;
                break;
            case "Газ":
                id = 2;
                break;
            case "Електроенергія":
                id = 3;
                break;
        }

        return id;
    }

    public String[] refreshListOfTariffs() {
        ArrayList<String> listOfTariffs = new ArrayList<>();

        listOfTariffs.add("Оберіть тариф:");
        while (tariffs_db.moveToNext()) {
            listOfTariffs.add(tariffs_db.getString(1));
        }
        listOfTariffs.add("Додати новий тариф");

        tariffs = listOfTariffs.toArray(new String[0]);
        ArrayAdapter<String> tariffsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, tariffs);
        chooseTariff.setAdapter(tariffsAdapter);

        return tariffs;
    }

    public boolean currentValidate() {
        String service = chooseService.getSelectedItem().toString();
        String tariff = chooseTariff.getSelectedItem().toString();
        String date = chooseMonth.getSelectedItem().toString();

        boolean result = !service.equals("Оберіть сервіс:") &&
                !tariff.equals("Оберіть тариф:") &&
                !date.equals("Оберіть місяць:");

        currentReadings.setFocusableInTouchMode(result);
        currentReadings.setFocusable(result);

        return result;
    }

    public void sumCalculate() {
        String service = chooseService.getSelectedItem().toString();
        String tariff = chooseTariff.getSelectedItem().toString();
        String date = chooseMonth.getSelectedItem().toString();
        String previousDate = Arrays.asList(mainActivity.months)
                .indexOf(date) - 1 + "." + Calendar.getInstance().get(Calendar.YEAR);

        if (service.equals("Оберіть сервіс:") || tariff.equals("Оберіть тариф:") || date.equals("Оберіть місяць:")) {
            return;
        }

        int previous = db.getRecordPrevious(service, previousDate);

        int current;
        try {
            current = Integer.parseInt(currentReadings.getText().toString());
        }
        catch (NumberFormatException e) {
            sum.setText("0 грн");
            return;
        }

        float price = db.getTariffPrice(tariff);

        sum.setText((current - previous) * price + " грн");
    }
}