import React from 'react';
import { View, StyleSheet, Clipboard } from 'react-native';
import { Text, Card, ActivityIndicator, Button } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import * as ClipboardAPI from 'expo-clipboard';
import { useAuth } from '../lib/AuthContext';
import ScreenHeader from './ScreenHeader';
import { useNotification } from '../components/NotificationContext';

export default function Account() {
    const { user, logout } = useAuth();
    const router = useRouter();
    const { notify } = useNotification();

    const handleLogout = async () => {
        await logout();
        router.replace('/login');
    };

    const copyToken = async () => {
        if (user?.token) {
            await ClipboardAPI.setStringAsync(user.token);
            notify({ type: 'success', message: 'Token copied to clipboard!' });
        }
    };

    if (!user) {
        return (
            <SafeAreaView style={styles.centered}>
                <ActivityIndicator size="large" />
                <Text style={{ marginTop: 12 }}>Loading account...</Text>
            </SafeAreaView>
        );
    }

    return (
        <SafeAreaView style={styles.safeArea}>
            <ScreenHeader title="Account" />
            <View style={styles.content}>
                <Card style={styles.card}>
                    <Card.Title title="Account Info" />
                    <Card.Content>
                        <Text style={styles.label}>Name:</Text>
                        <Text>{user.name.toUpperCase()}</Text>

                        <Text style={styles.label}>Phone:</Text>
                        <Text>{user.phone.toUpperCase()}</Text>

                        <Text style={styles.label}>Role:</Text>
                        <Text>{user.role.toUpperCase()}</Text>

                        {user.role === 'bakery' && user.token && (
                            <>
                                <Text style={styles.label}>API Token:</Text>
                                <Text selectable style={{ fontFamily: 'monospace' }}>
                                    {user.token}
                                </Text>
                                <Button
                                    mode="outlined"
                                    style={styles.copyBtn}
                                    onPress={copyToken}
                                >
                                    Copy Token
                                </Button>
                            </>
                        )}
                    </Card.Content>
                </Card>
                <Button
                    mode="outlined"
                    style={styles.logoutBtn}
                    onPress={handleLogout}
                >
                    Logout
                </Button>
            </View>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#fff' },
    centered: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#fff',
    },
    content: { padding: 20 },
    card: { borderRadius: 10, elevation: 3, backgroundColor: '#fff' },
    label: { fontWeight: 'bold', marginTop: 12 },
    logoutBtn: {
        marginTop: 20,
    },
    copyBtn: {
        marginTop: 12,
    },
});