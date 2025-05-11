// app/lib/NotificationContext.tsx
import React, { createContext, useContext, useState, ReactNode } from 'react';
import { Snackbar } from 'react-native-paper';

type NotificationType = 'success' | 'error' | 'info';

type Notification = {
    visible: boolean;
    message: string;
    type: NotificationType;
};

type NotificationContextType = {
    notify: (opts: { message: string; type?: NotificationType }) => void;
};

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

export const useNotification = () => {
    const context = useContext(NotificationContext);
    if (!context) throw new Error('useNotification must be used within NotificationProvider');
    return context;
};

export const NotificationProvider = ({ children }: { children: ReactNode }) => {
    const [notification, setNotification] = useState<Notification>({
        visible: false,
        message: '',
        type: 'info',
    });

    const notify = ({ message, type = 'info' }: { message: string; type?: NotificationType }) => {
        setNotification({ visible: true, message, type });
    };

    const onDismiss = () => setNotification(prev => ({ ...prev, visible: false }));

    const getBackgroundColor = () => {
        switch (notification.type) {
            case 'error': return '#e53935';
            case 'success': return '#43a047';
            case 'info':
            default: return '#2196f3';
        }
    };

    return (
        <NotificationContext.Provider value={{ notify }}>
            {children}
            <Snackbar
                visible={notification.visible}
                onDismiss={onDismiss}
                duration={4000}
                action={{ label: 'OK', onPress: onDismiss }}
                style={{ backgroundColor: getBackgroundColor() }}
            >
                {notification.message}
            </Snackbar>
        </NotificationContext.Provider>
    );
};