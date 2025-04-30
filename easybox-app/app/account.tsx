import { useEffect, useState } from 'react';
import { View, StyleSheet } from 'react-native';
import { Text } from 'react-native-paper';
import api from '../lib/api';
import ScreenHeader from './ScreenHeader';

type User = {
    name: string;
    email: string;
    role: string;
};

export default function Account() {
    const [user, setUser] = useState<User | null>(null);

    useEffect(() => {
        api.get('/users/me').then(res => setUser(res.data)).catch(() => {});
    }, []);

    if (!user) return <Text>Loading...</Text>;

    return (
        <View style={styles.container}>
            <ScreenHeader title="Account" />
            <View style={styles.content}>
                <Text>Name: {user.name}</Text>
                <Text>Email: {user.email}</Text>
                <Text>Role: {user.role}</Text>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1 },
    content: { padding: 20 },
});
