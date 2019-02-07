package keys.fantom.fantomkeys;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final String ROOM_IS_NOT_AVAILABLE_TRY_TO_CHOOSE_ANOTHER_DATE = "Room is not available, try to choose another date.";
    public static final String ROOM_IS_PAYED = "Room is payed.";
    public static final String ROOM_IS_AVAILABLE_TO_RESERVE = "Room is available to reserve.";
    private boolean writeMode = false;
    boolean mWriteMode = false;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mNfcPendingIntent;
    private static String key = "87B69733C23BD3B8FEADA5B656DD9C5A6195632385EA05B09891BA7F3CBF8057";
    private static String address = "0xd3bee5ae53dfbc3a81e62ae2cba2cfe75cace6b5";
    private String[] accounts = new String[]{address};
    private Context context;
    private long roomId = 1625L;
    private TextView dateFrom;
    private TextView dateTo;
    private Contract contract = new Contract(address, "0xD0c040A4B1452Fa80044c8c9f173A2B356f1cb4c", "http://18.221.128.6:8080");
    private TextView reservationInfoTextView;
    private Spinner chooseAccount;
    private LinearLayout layoutOpenTheDoor;
    private LinearLayout layoutReserve;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        context = this;

        layoutOpenTheDoor = (LinearLayout) findViewById(R.id.openTheDoorView);
        layoutReserve = (LinearLayout) findViewById(R.id.reserveLayout);
        dateFrom = (TextView) findViewById(R.id.dateFromValue);
        dateTo = (TextView) findViewById(R.id.dateToValue);
        chooseAccount = (Spinner) findViewById(R.id.chooseAccount);
        setAccountChooser();

        reservationInfoTextView = (TextView) findViewById(R.id.reservationInfo);
        reservationInfoTextView.setText(checkAvailability());

        mNfcAdapter = NfcAdapter.getDefaultAdapter(MainActivity.this);
        mNfcPendingIntent = PendingIntent.getActivity(MainActivity.this, 0,
                new Intent(MainActivity.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        mNfcAdapter.setNdefPushMessageCallback(new ViewNdefMessageCallback(), this);
    }

    private void setAccountChooser() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, accounts);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        chooseAccount.setAdapter(adapter);
        chooseAccount.setPrompt("Choose account");
        chooseAccount.setSelection(0);
        chooseAccount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                Toast.makeText(getBaseContext(), "Position = " + position, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        chooseAccount.setSelection(0);
    }

    private String checkAvailability() {


        boolean isAvailable = false;
        String dateFromString = dateFrom.getText().toString();
        long from = getDateLongFromString(dateFromString);
        String dateToString = dateTo.getText().toString();
        long to = getDateLongFromString(dateToString);
        try {
            isAvailable = !contract.isBooked(roomId, from, to);
        } catch (IOException e) {
            Toast.makeText(context, "Error checking if room is available.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        if (isAvailable) {
            layoutOpenTheDoor.setVisibility(View.GONE);
            layoutReserve.setVisibility(View.VISIBLE);
            return ROOM_IS_AVAILABLE_TO_RESERVE;
        } else {
            if (checkIfReservedByUser()) {
                layoutReserve.setVisibility(View.GONE);
                layoutOpenTheDoor.setVisibility(View.VISIBLE);
                return ROOM_IS_PAYED + "Your reservation is from " + dateFromString + " to " + dateToString;
            } else {
                layoutOpenTheDoor.setVisibility(View.GONE);
                layoutReserve.setVisibility(View.VISIBLE);
                return ROOM_IS_NOT_AVAILABLE_TRY_TO_CHOOSE_ANOTHER_DATE;
            }
        }
    }

    private static boolean checkIfReservedByUser() {
        //TODO
        return true;
    }

    private long getDateLongFromString(String dateString) {
        DateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
        Date date = null;

        try {
            date = formatter.parse(dateString);
        } catch (ParseException e) {
            Toast.makeText(getBaseContext(), "Date is need to be in format DD.MM.YYYY.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        return date.getTime();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (writeMode) {
            disableTagWriteMode();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        enableTagWriteMode();
    }


    private void enableTagWriteMode() {
        mWriteMode = true;

        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        IntentFilter[] writeTagFilters = new IntentFilter[]{tagDetected, techDetected};

        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, writeTagFilters, null);
    }

    private void disableTagWriteMode() {
        mWriteMode = false;
        mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // Tag writing mode
        if (mWriteMode && (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()))) {
            mNfcAdapter.setNdefPushMessageCallback(new ViewNdefMessageCallback(), this);
        }
    }

    public void showDatePicker(View view) {
    }

    public void reserve(View view) {
        long from = getDateLongFromString(dateFrom.getText().toString());
        long to = getDateLongFromString(dateTo.getText().toString());
        try {
            contract.book(roomId, from, to, new BigInteger(key, 16));
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Error booking the room.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (org.json.simple.parser.ParseException e) {
            Toast.makeText(getBaseContext(), "Error booking the room.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        Date waitStart = new Date();
        while (true) {
            String availableStatusMessage = checkAvailability();
            reservationInfoTextView.setText(availableStatusMessage);
            if (availableStatusMessage.startsWith(ROOM_IS_PAYED)) {
                return;
            }
            Date check = new Date();
            if (check.getTime() - waitStart.getTime() > 15000) {
                Toast.makeText(getBaseContext(), "Timout waiting booking execution.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }

    class ViewNdefMessageCallback implements NfcAdapter.CreateNdefMessageCallback {

        @Override
        public NdefMessage createNdefMessage(NfcEvent event) {
            chooseAccount.getSelectedItem().toString();

            String deviceId = UUID.randomUUID().toString();

            Object[] data = {deviceId.getBytes()};
            SignaApp.sign(SignaApp.createKeyPair(key), data);
            NdefRecord record = NdefRecord.createMime("text/plain", key.getBytes());

            NdefMessage message = new NdefMessage(new NdefRecord[]{record});
            return message;
        }
    }
}
