import { useEffect, useState } from 'react';
import { View, StyleSheet } from 'react-native';
import { Text, Card, ActivityIndicator, Title, Button } from 'react-native-paper';
import api from '../lib/api';
import ScreenHeader from './ScreenHeader';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { removeToken } from '../lib/auth';
export type User = {
    name: string;
    email: string;
    role: string;
};


export default function Account() {
    const [user, setUser] = useState<User | null>(null);
    const router = useRouter();
    const handleLogout = async () => {
        await removeToken();
        router.replace('/login');
    };
    useEffect(() => {
        api.get('/users/me')
            .then(res => setUser(res.data))
            .catch(() => setUser(null));
    }, []);


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
                        <Text>{user.name}</Text>

                        <Text style={styles.label}>Email:</Text>
                        <Text>{user.email}</Text>

                        <Text style={styles.label}>Role:</Text>
                        <Text>{user.role}</Text>
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
});
