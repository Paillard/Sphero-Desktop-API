/*
 * Please read the LICENSE file that is included with the source
 * code.
 */

package experimental.sensor;

import se.nicklasgavelin.sphero.Robot;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

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
        sensordata = new LinkedList<>();
        listeners = new LinkedList<>();
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
            sensordata.add(ax);

            if( !started)
            {
                schedule( new TouchCheckEvent(), eventDelay);
            }
        }
    }

    public void addTouchListener( TouchListener l )
    {
        if( !listeners.contains(l) )
            listeners.add(l);
    }

    public void removeTouchListener( TouchListener l )
    {
        listeners.remove(l);
    }

    public void notifyListeners()
    {
        for( TouchListener l : listeners)
            l.touchEvent(r);
    }

    private long touched;
    public class TouchCheckEvent extends TimerTask
    {
        @Override
        public void run()
        {
            synchronized(sensordata)
            {
                if(sensordata.isEmpty() )
                {
                    started = false;
                    return;
                }

                // Do something with the data
                int dX = 0;
                int dY = 0;
                int dZ = 0;

                for( AccelerometerSensorData s : sensordata)
                {
                    dX += s.getAxis3Sensor().x;
                    dY += s.getAxis3Sensor().y;
                    dZ += s.getAxis3Sensor().z;
                }

                dX = dX / sensordata.size();
                dY = dY / sensordata.size();
                dZ = dZ / sensordata.size();

                // Now check if it's above our threshold
                double k = Math.sqrt(dX*dX + dY*dY + dZ*dZ);
//                System.out.println( "k=" + k );
                if( k > threshold)
                {
                    long time = System.currentTimeMillis();
                    long BACKOFF = 1500;
                    if(  time - touched > BACKOFF)
                    {
                        // Update touched time
                        touched = time;

                        // Notify listeners
                        notifyListeners();
                    }
                }

                sensordata.clear();

                // Set started to false
                started = false;
            }
        }
    }

    public interface TouchListener
    {
        void touchEvent(Robot r);
    }
}
