package autocoupons.com.webviewapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import autocoupons.com.webviewapp.receivers.OverlayObservable;
import autocoupons.com.webviewapp.receivers.WebviewObservable;


public class MyDialog extends AppCompatActivity implements Observer {
    public static boolean active = false;
    private String myntraCouponString = "MYNTRANEW300~MYNTRANEW1000~MYNTRANEW600" +
            "~SKECH1000~SKECH2500~DESIFUSION20~SPECIALBIG20~VEROMODA10~";
    private int count;
    public static Activity myDialog;
    private ArrayList<CouponItem> myntraCoupons = new ArrayList<>();
    private WeakReference<AsyncTaskHelper> myntraAsyncWeakRef;

    private LinearLayout rootLayout;
    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;
    private TextView couponStatus;
    private ProgressBar progressBar;
    private TextView message;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_my_dialog);
        myDialog = MyDialog.this;
        initLayoutChildren();
        executeAsyncTaskToGetCoupons();

    //top.setOnClickListener(new OnClickListener() {
    //
    //            @Override
    //            public void onClick(View v) {
    //                // TODO Auto-generated method stub
    //                finish();
    //            }
    //        });

    }

    private void initLayoutChildren() {
        couponStatus = (TextView)findViewById(R.id.status);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        rootLayout = (LinearLayout) findViewById(R.id.parent);
        message = (TextView) findViewById(R.id.textView2);
        recyclerView = (RecyclerView) rootLayout.findViewById(R.id.couponList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return true;
            }
        });
        recyclerViewAdapter = new RecyclerViewAdapter(new ArrayList<CouponItem>());
        recyclerView.setAdapter(recyclerViewAdapter);
    }

    private void executeAsyncTaskToGetCoupons() {
        if(!isAsyncTaskPendingorRunning()) {
            final AsyncTaskHelper myntraCoup = new AsyncTaskHelper(2){
                @Override
                protected void onPostExecute(String s) {
                    if(!s.isEmpty()) {
                        myntraCouponString = s;
                        for(String a : s.split("~"))
                            myntraCoupons.add(new CouponItem(a, "Not Applicable"));
                        recyclerViewAdapter.setData(myntraCoupons);
                        Log.d("broadcast", s);
//                        progressBar.setMax(myntraCoupons.size());
                        Intent intent = new Intent(AutoCouponsHeadService.OverlayToSvcReceiver.OVERLAY_TO_SVC);
                        intent.putExtra("COUPONS", s);
                        LocalBroadcastManager.getInstance(MyDialog.this).sendBroadcast(intent);
                    }
                    super.onPostExecute(s);
                }
            };
            myntraAsyncWeakRef = new WeakReference<AsyncTaskHelper>(myntraCoup);
            myntraCoup.execute();
        }
    }

    private boolean isAsyncTaskPendingorRunning() {
        return this.myntraAsyncWeakRef != null &&
                this.myntraAsyncWeakRef.get() != null &&
                !this.myntraAsyncWeakRef.get().getStatus().equals(AsyncTask.Status.FINISHED);
    }


    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        active = true;
        OverlayObservable.getInstance().addObserver(this);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        active = false;
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        active = false;
        OverlayObservable.getInstance().deleteObserver(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void update(Observable observable, final Object o) {
        if(count >= myntraCoupons.size()) {
            Log.d("rv", (String)o);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    message.setText("");
                    message.setVisibility(View.VISIBLE);
                    if(!((String) o).split("~")[1].equals("-1")) {
                        int idx = getIndexOf(((String) o).split("~")[0]);
                        message.setText("You got "+ ((String) o).split("~")[1]+" discount! ");
                        CouponItem temp = myntraCoupons.get(0);
                        myntraCoupons.set(0, myntraCoupons.get(idx));
                        myntraCoupons.set(idx, temp);
                        recyclerViewAdapter.setData(myntraCoupons);
                    } else {
                        message.setText("Sorry! no applicable coupons found. ");
                    }
                }
            });
            return;
        }
        progressBar.setProgress((int)(count*100/(myntraCoupons.size()-1)), true);
        count++;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String s = ((String)o).split("~")[0];
                if(!s.isEmpty()) {
                    CouponItem temp = new CouponItem(s, "");
                    int pos = getIndexOf(s);
                    if(pos >= 0) {
                        myntraCoupons.get(pos).setApplicability("Applicable");
                        Log.d("rv", myntraCoupons.get(pos).toString());
                    }
                }
                couponStatus.setText(count + "/"+ myntraCoupons.size() + " checked");
            }
        });
    }

    private int getIndexOf(String s) {
        Log.d("rv", s);
        for(int i=0; i<myntraCoupons.size(); i++) {
            if(myntraCoupons.get(i).getCoupon().equals(s))
                return i;
        }
        return -1;
    }

    private static class AsyncTaskHelper extends AsyncTask<Void, Void, String> {

        private final static String URL = "http://coupons.buyhatke.com/PickCoupon/FreshCoupon/getCoupons.php";
        private final String dataUrl;

        public AsyncTaskHelper(int pos) {
            this.dataUrl = URL+"?pos="+pos;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                java.net.URL url = new URL(dataUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();

                String line;
                while((line = br.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }
    }

    private class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

        private ArrayList<CouponItem> data;

        public RecyclerViewAdapter(ArrayList<CouponItem> strings) {
            this.data = strings;
        }


        @Override
        public RecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View rootView = getLayoutInflater().inflate(R.layout.coupon, parent, false);
            return new ViewHolder(rootView);
        }

        @Override
        public void onBindViewHolder(RecyclerViewAdapter.ViewHolder holder, int position) {
            holder.couponName.setText(data.get(position).getCoupon());

            if(data.get(position).getApplicability().equals("Applicable")) {
                holder.isApplicable.setTextColor(Color.GREEN);
                holder.isApplicable.setText(data.get(position).getApplicability());
            } else {
                holder.isApplicable.setText(data.get(position).getApplicability());
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public void setData(ArrayList<CouponItem> data) {
            this.data = data;
            notifyDataSetChanged();
        }

        public void setApplicationStatus(String s) {

        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView couponName;
            private TextView isApplicable;

            public ViewHolder(View itemView) {
                super(itemView);
                if(itemView instanceof LinearLayout) {
                    couponName = (TextView) itemView.findViewById(R.id.name);
                    isApplicable = (TextView) itemView.findViewById(R.id.is_applied);
                }
            }
        }

    }

    private class CouponItem {
        private String coupon;
        private String applicability;

        public CouponItem(String coupon, String applicability) {
            this.coupon = coupon;
            this.applicability = applicability;
        }

        public String getCoupon() {
            return coupon;
        }

        public String getApplicability() {
            return applicability;
        }

        public void setCoupon(String coupon) {
            this.coupon = coupon;
        }

        public void setApplicability(String applicability) {
            this.applicability = applicability;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CouponItem)) return false;

            CouponItem that = (CouponItem) o;

            if (!getCoupon().equals(that.getCoupon())) return false;
            return getApplicability().equals(that.getApplicability());

        }

        @Override
        public int hashCode() {
            int result = getCoupon().hashCode();
            result = 31 * result + getApplicability().hashCode();
            return result;
        }

        @Override
        public String toString() {
            return this.coupon+" is "+this.applicability;
        }
    }

}



