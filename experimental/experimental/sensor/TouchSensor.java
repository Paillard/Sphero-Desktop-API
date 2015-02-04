/*
 * Please read the LICENSE file that is included with the source
 * code.
 */

package experimental.sensor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import se.nicklasgavelin.sphero.Robot;

/**
 *
 * @author Nicklas Gavelin, nicklas.gavelin@gmail.com, Luleå University of Technology
 */
public class TouchSensor extends Timer
{
    private final Collection<TouchListener> listeners;
    private final Robot r;
    private static final int threshold = 800;
    private final Collection<AccelerometerSensorData> sensordata;
    private boolean started;
    private final int eventDelay;
    private static final int DEFAULT_EVENT_DELAY = 500;

    public TouchSensor( Robot r, int eventDelay )
    {
        this.r = r;
        this.sensordata = new LinkedList<>();
        this.listeners = new LinkedList<>();
        this.eventDelay = eventDelay;
    }

    public TouchSensor( Robot r )
    {
        this( r, DEFAULT_EVENT_DELAY);
    }

    public void addData( AccelerometerSensorData ax )
    {
        synchronized(sensordata)
        {
            this.sensordata.add(ax);

            if( !this.started)
            {
                this.schedule( new TouchCheckEvent(), eventDelay);
            }
        }
    }

    public void addTouchListener( TouchListener l )
    {
        if( !this.listeners.contains(l) );
        this.listeners.add(l);
    }

    public void removeTouchListener( TouchListener l )
    {
        this.listeners.remove(l);
    }

    public void notifyListeners()
    {
        for( TouchListener l : this.listeners)
            l.touchEvent(r);
    }

    private long touched;
    public class TouchCheckEvent extends TimerTask
    {
        @Override
        public void run()
        {
            synchronized(TouchSensor.this.sensordata)
            {
                if(TouchSensor.this.sensordata.isEmpty() )
                {
                    TouchSensor.this.started = false;
                    return;
                }

                // Do something with the data
                int dX = 0;
                int dY = 0;
                int dZ = 0;

                for( AccelerometerSensorData s : TouchSensor.this.sensordata)
                {
                    dX += s.getAxis3Sensor().x;
                    dY += s.getAxis3Sensor().y;
                    dZ += s.getAxis3Sensor().z;
                }

                dX = dX / TouchSensor.this.sensordata.size();
                dY = dY / TouchSensor.this.sensordata.size();
                dZ = dZ / TouchSensor.this.sensordata.size();

                // Now check if it's above our threshold
                double k = Math.sqrt(dX*dX + dY*dY + dZ*dZ);
//                System.out.println( "k=" + k );
                if( k > threshold )
                {
                    long time = System.currentTimeMillis();
                    long BACKOFF = 1500;
                    if(  time - touched > BACKOFF)
                    {
                        // Update touched time
                        TouchSensor.this.touched = time;

                        // Notify listeners
                        TouchSensor.this.notifyListeners();
                    }
                }

                TouchSensor.this.sensordata.clear();

                // Set started to false
                TouchSensor.this.started = false;
            }
        }
    }

    public interface TouchListener
    {
        void touchEvent(Robot r);
    }
}
