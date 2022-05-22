package com.example.ekomunalka;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RecordActivity extends AppCompatActivity {

    DatabaseHelper db;
    MainActivity mainActivity;
    Spinner chooseMonth;
    Spinner chooseService;
    EditText currentReadings;
    CheckBox isPaid;
    EditText commentText;
    Button saveData;

    Cursor receivedItem;
    int id;

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
        String[] services = new String[] { "Вода", "Газ", "Електроенергія" };
        ArrayAdapter<String> servicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, services);
        chooseService.setAdapter(servicesAdapter);
        isPaid = findViewById(R.id.isPaid);
        saveData = findViewById(R.id.saveData);
        commentText = findViewById(R.id.comment);

        Intent receivedIntent = getIntent();
        id = (int) receivedIntent.getLongExtra("id", -1);
        receivedItem = db.getItem(id);

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

        saveData.setOnClickListener(v -> {
            String date = Arrays.asList(mainActivity.months)
                    .indexOf(chooseMonth.getSelectedItem().toString()) + "." + Objects.requireNonNull(data.get("date")).substring(Objects.requireNonNull(data.get("date")).length() - 4);
            String service = chooseService.getSelectedItem().toString();
            String current = currentReadings.getText().toString();
            String paid = isPaid.isChecked() ? "1" : "0";
            String comment = commentText.getText().toString();

            Map<String, String> newValues = GetDataFromLocal(date, service, current, paid, comment);

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
        boolean insertData = db.UpdateData(newValues, id);

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

    public Map<String, String> GetDataFromLocal(String date, String service, String current, String paid, String comment) {
        Map<String, String> data = new HashMap<>();

        data.put("date", date);
        data.put("service", service);
        data.put("current", current);
        data.put("paid", paid);
        data.put("comment", comment);

        return data;
    }

    public int GetServiceId(String service) {
        int id = -1;

        switch (service) {
            case "Вода":
                id = 0;
                break;
            case "Газ":
                id = 1;
                break;
            case "Електроенергія":
                id = 2;
                break;
        }

        return id;
    }
}