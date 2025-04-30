// app/redirect.tsx
import { useRouter, useLocalSearchParams } from 'expo-router';
import { useEffect } from 'react';
import { View, ActivityIndicator } from 'react-native';
import { Text } from 'react-native-paper';

export default function Redirect() {
    const { role } = useLocalSearchParams<{ role: string }>();
    const router = useRouter();

    useEffect(() => {
        const timeout = setTimeout(() => {
            if (role === 'maintenance') {
                router.replace({ pathname: '/maintenance' });
            } else {
                router.replace({ pathname: '/home', params: { role } });
            }
        }, 1000); // delay for animation/smoothness

        return () => clearTimeout(timeout);
    }, [role]);

    return (
        <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
            <ActivityIndicator size="large" />
            <Text style={{ marginTop: 16 }}>Logging in as {role}...</Text>
        </View>
    );
}
