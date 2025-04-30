import { useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { View, StyleSheet } from 'react-native';
import { Text } from 'react-native-paper';
import api from '../lib/api';
import ScreenHeader from './ScreenHeader';

type Order = {
    id: string;
    status: string;
    createdAt: string;
    items: { name: string; quantity: number }[];
};

export default function OrderDetails() {
    const { id } = useLocalSearchParams();
    const [order, setOrder] = useState<Order | null>(null);

    useEffect(() => {
        api.get(`/orders/${id}`).then(res => setOrder(res.data)).catch(() => {});
    }, [id]);

    if (!order) return <Text>Loading...</Text>;

    return (
        <View style={styles.container}>
            <ScreenHeader title="Order Details" showBack />
            <View style={styles.content}>
                <Text>Order ID: {order.id}</Text>
                <Text>Status: {order.status}</Text>
                <Text>Placed at: {order.createdAt}</Text>
                <Text>Items:</Text>
                {order.items.map((item, i) => (
                    <Text key={i}>- {item.name} x {item.quantity}</Text>
                ))}
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1 },
    content: { padding: 20 },
});
