import 'react-leaflet';
import { Icon } from 'leaflet';

declare module 'react-leaflet' {
    // Augment the MarkerProps interface to include an icon property.
    interface MarkerProps {
        icon?: Icon<any>;
    }
}
