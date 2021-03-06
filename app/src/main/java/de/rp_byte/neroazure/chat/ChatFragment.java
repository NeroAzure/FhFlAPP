package de.rp_byte.neroazure.chat;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

import com.example.antonimuller.fhflapp.R;

/**
 * Fragment für den Chat, instanziert den Datagram Service und holt sich ein Multicastlock.
 * Ist auh der ReceiveListener für den DatagramService.
 * created by neroazure
 */

public class ChatFragment extends Fragment implements View.OnClickListener,IReceiveListener{
    private final String TAG = "RPB/ChatFrag";

    private DatagramService serv;

    private View fragmentView;

    WifiManager.MulticastLock lock;

    EditText input;
    EditText name;
    Button send;
    ListView lm;
    MessageAdapter ma;

    ArrayList<Message> Messages = new ArrayList<>();

    public ChatFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.chat_fragment, container, false);

        input = (EditText) fragmentView.findViewById(R.id.message);
        input.addTextChangedListener(getWatcher(992,"Message",false));

        name = (EditText) fragmentView.findViewById(R.id.name);
        name.addTextChangedListener(getWatcher(32,"Name",false));

        send = (Button) fragmentView.findViewById(R.id.button);
        send.setOnClickListener(this);

        ma = new MessageAdapter(getActivity(),R.layout.message,Messages);
        lm = (ListView) fragmentView.findViewById(R.id.chat);
        lm.setAdapter(ma);

        try {
            // erstellen und starten des Datagramservices
            serv = new DatagramService(this,getActivity());

            serv.start();
        } catch (SocketException e) {
            Log.e(TAG, "onCreateView: an Exception occurred", e);
        }

        return fragmentView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //Holen des Multicastlocks
        WifiManager wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        lock = wifi.createMulticastLock("RPB");
        lock.acquire();
    }


    @Override
    public void onPause(){
        super.onPause();
        serv.stop();
        //Freigeben des Locks
        if (lock.isHeld())
            lock.release();
    }


    //Click-Listener
    @Override
    public void onClick(View v) {
        String message = input.getText().toString();
        String name_ = name.getText().toString();

        //gesendet wird nur wenn auch eine Nachricht da ist,
        //sowie auch ein Name
        if (message.length() > 0 && name_.length() > 0){
            input.setText("");

            byte[] namepadded = pad(name_.getBytes(Charset.forName("UTF-8")),32);
            byte[] messagepadded = pad(message.getBytes(Charset.forName("UTF-8")),992);

            //Debug und senden des Texts
            Log.i(TAG, "onClick: Sent? "+serv.send(concat(namepadded,messagepadded)));
        }
    }


    //geliehen von http://stackoverflow.com/a/80503
    public byte[] concat(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;
        byte[] c= new byte[aLen+bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    //die ist selber geschrieben
    public byte[] pad(byte[] in, int length){
        byte[] padded = new byte[length];
        System.arraycopy(in,0,padded,0,in.length);
        return padded;
    }

    //Textwatcher zum überprüfen der Byte-Länge der EditFelder
    public TextWatcher getWatcher(final int length, final String forn, final boolean debug){
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String name = s.toString();

                if(debug)
                    Log.i(TAG, "afterTextChanged for "+forn+": Length is "+
                            name.getBytes(Charset.forName("UTF-8")).length+" Bytes and "+name.length()+" Characters");

                if(name.getBytes().length > length){
                    s.delete(s.length()-1,s.length());
                }
            }
        };
    }

    //Handler fürs empfangen von Nachrichten
    @Override
    public void handleMessage(byte[] message) {

        int endindexn = 32;
        for (int i = 0; i < 32;i++) {
            if(message[i] == 0){
                endindexn = i;
                break;
            }
        }


        int endindexm = 1024;
        for (int i = 32; i < 1024; i++){
            if(message[i] == 0){
                endindexm = i;
                break;
            }
        }

        String sender = new String(Arrays.copyOfRange(message,0,endindexn), Charset.forName("UTF-8"));
        String message_ = new String(Arrays.copyOfRange(message,32,endindexm), Charset.forName("UTF-8"));

        Messages.add(new Message(sender,message_));
        ma.notifyDataSetChanged();

        Log.i(TAG, "handleMessage: Message received: \n"+
                sender+ " : " + message_);
    }
}
