package com.example.testegertec;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import br.com.gertec.gedi.GEDI;
import br.com.gertec.gedi.enums.GEDI_PRNTR_e_Alignment;
import br.com.gertec.gedi.enums.GEDI_PRNTR_e_BarCodeType;
import br.com.gertec.gedi.exceptions.GediException;
import br.com.gertec.gedi.interfaces.IGEDI;
import br.com.gertec.gedi.interfaces.IPRNTR;
import br.com.gertec.gedi.structs.GEDI_PRNTR_st_BarCodeConfig;
import br.com.gertec.gedi.structs.GEDI_PRNTR_st_PictureConfig;
import br.com.gertec.gedi.structs.GEDI_PRNTR_st_StringConfig;


public class MainActivity extends AppCompatActivity {
    private NfcAdapter nfcAdapter;
    private MifareClassic mifareClassic;
    private Tag tag;
    private TextView textView;

    private Button imprimir;
    private Button ler_qrCode;
    private IGEDI igedi;
    private IPRNTR iprint;
    private GEDI_PRNTR_st_StringConfig stringConfig;
    private GEDI_PRNTR_st_PictureConfig pictureConfig;
    private Typeface typeface;
    private IntentIntegrator qrScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ler_qrCode = findViewById(R.id.btn_leitor);
        ler_qrCode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               startCamera();
            }
        });
        textView = findViewById(R.id.hello);

        imprimir = findViewById(R.id.btn_imprimir);
        imprimir.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                imprimirNota();
            }
        });


        new Thread(new Runnable() {
            @Override
            public void run() {
                GEDI.init(getApplicationContext());
                igedi = GEDI.getInstance(getApplicationContext());
                iprint = igedi.getPRNTR();
            }
        }).start();
    }

    protected void OnStart(){
        super.onStart();
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }
    protected void OnResume(){
        super.onResume();
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefDected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techDected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter idDetected = new IntentFilter(NfcAdapter.EXTRA_ID);
        IntentFilter [] nfcIntentFilter = new IntentFilter[]{techDected,tagDetected, ndefDected,idDetected};
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this,
                        getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),0);
        if(nfcAdapter!=null){
            nfcAdapter.enableForegroundDispatch(this, pendingIntent,nfcIntentFilter,null);
        }
    }
    protected  void OnNewIntent(Intent intent){
        super.onNewIntent(intent);
        try{
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if(tag == null){
                Toast.makeText(getApplicationContext(), "Nao foi possivel ler o cartao", Toast.LENGTH_LONG).show();
            }else{
                // LerCartao();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    protected void LerCartao(){
        mifareClassic = MifareClassic.get(tag);
        textView.setText("Leitura: "+idCartao());
    }
    public String idCartao(){
        byte[] idCartao = mifareClassic.getTag().getId();
        long result = 0;
        if(idCartao == null) return "";
        for(int i =  idCartao.length-1; i >= 0;--i){
            result <<= 8;
            result |= idCartao[i] & 0x0FF;
        }
        return Long.toString(result);
    }
    private void startCamera() {
        qrScan = new IntentIntegrator(this);
        qrScan.setPrompt("Digitalizar o codigo");
        qrScan.setBeepEnabled(true);
        qrScan.setBarcodeImageEnabled(true);
        qrScan.setTimeout(30000);
        qrScan.initiateScan();
    }
    public void pulaLinha(int quantidade) {
        try {
            this.iprint.DrawBlankLine(quantidade);
        } catch (GediException e) {
            e.printStackTrace();
        }
    }
    public void impressao(String texto, int size, String alinhar) {
        this.stringConfig = new GEDI_PRNTR_st_StringConfig(new Paint());
        this.stringConfig.paint.setTextSize(size);
        this.stringConfig.paint.setTextAlign(Paint.Align.valueOf(alinhar));
        this.stringConfig.offset = 2;
        this.stringConfig.lineSpace = 1;
        this.typeface = Typeface.create(Typeface.DEFAULT, typeface.NORMAL);
        typeface = Typeface.create(typeface, Typeface.BOLD_ITALIC);
        this.stringConfig.paint.setTypeface(this.typeface);
        try {
            this.iprint.DrawStringExt(this.stringConfig, texto);
        } catch (GediException e) {
            e.printStackTrace();
        }
    }
    private String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
    public void imprimirNota(){
        String texto  = "";

        for(int i = 0; i < 15;i++){
            if(i==14)
                texto+= "Produto Teste Aleatorio: " + (i+1) + " Valor - R$"+(i+1)*200;
            else
                texto+= "Produto Teste Aleatorio: " + (i+1) + " Valor - R$"+(i+1)*200+"\n";
        }
        try {
            this.iprint.Init();
            this.impressao("Nota Fiscal Eletrônica - NFe\n", 30, "CENTER");
            this.impressao("Data: "+this.getDateTime()+"\n", 30, "LEFT");
            this.impressao(texto,15, "LEFT");
            this.imprimirCodigodeBarra("7898357410015", 400, 400, "QR_CODE");
            this.imprimirCodigodeBarra("7898357410015", 100, 400, "EAN_13");
            this.pulaLinha(70);
            this.imprimeImagem("neigh");
            this.pulaLinha(30);
            this.impressao("Valor Total Da Compra",30,"CENTER");
            this.pulaLinha(150);
            this.iprint.Output();
        } catch (GediException e) {
            e.printStackTrace();
        }
    }
    public boolean imprimeImagem(String image) {
        try {
            pictureConfig = new GEDI_PRNTR_st_PictureConfig();
            pictureConfig.alignment = GEDI_PRNTR_e_Alignment.valueOf("CENTER");
            pictureConfig.height = 400;
            pictureConfig.width = 400;
            int id = getApplicationContext().getResources().getIdentifier(image, "drawable", getApplicationContext().getPackageName());
            Bitmap bmp = BitmapFactory.decodeResource(getApplicationContext().getResources(), id);
            this.iprint.DrawPictureExt(pictureConfig, bmp);
            return true;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (GediException e) {
            e.printStackTrace();
        }
        return false;
    }
    public boolean imprimirCodigodeBarra(String texto, int height, int width, String barCodeType){
        try {
            GEDI_PRNTR_st_BarCodeConfig barCodeConfig = new GEDI_PRNTR_st_BarCodeConfig();
            barCodeConfig.barCodeType = GEDI_PRNTR_e_BarCodeType.valueOf(barCodeType);
            barCodeConfig.height = height;
            barCodeConfig.width = width;
            this.iprint.DrawBarCode(barCodeConfig, texto);
        }catch (IllegalArgumentException e){
            throw new IllegalArgumentException(e);
        } catch (GediException e) {
            e.printStackTrace();
        }
        return true;
    }
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intresult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (intresult != null) {
            if (intresult.getContents() == null) {
                Toast.makeText(getApplicationContext(), "Result Not Found", Toast.LENGTH_LONG);
            } else {
                try {
                    Toast.makeText(getApplicationContext(), intresult.getContents(), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}