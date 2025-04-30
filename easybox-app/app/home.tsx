import { useEffect, useState } from 'react';
import { FlatList, View, StyleSheet } from 'react-native';
import {
    Text,
    Button,
    ActivityIndicator,
    Chip,
    Title,
    Divider,
    Surface,
    TouchableRipple,
} from 'react-native-paper';
import { useRouter } from 'expo-router';
import api from '../lib/api';
import { SafeAreaView } from 'react-native-safe-area-context';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import ScreenHeader from '@/app/ScreenHeader';
import { useAuth } from '../lib/AuthContext';

type Order = {
    id: string;
    status: string;
    deliveryTime: string;
    easyboxAddress: string;
};

function getStatusChipStyle(status: string) {
    return {
        backgroundColor:
            status === 'place order'
                ? '#ff9800'
                : status === 'pickup order'
                    ? '#4caf50'
                    : '#ccc',
        marginRight: 8,
    };
}

export default function HomeScreen() {
    const router = useRouter();
    const { role, userId } = useAuth();
    const [orders, setOrders] = useState<Order[] | null>(null);
    const [showPastOrders, setShowPastOrders] = useState(false);

    useEffect(() => {
        api.get('/orders', { params: { userId, role } })
            .then(res => setOrders(res.data))
            .catch(() => setOrders([]));
    }, [userId, role]);

    if (!orders) {
        return (
            <SafeAreaView style={styles.centered}>
                <ActivityIndicator size="large" />
                <Text style={{ marginTop: 12 }}>Loading your orders...</Text>
            </SafeAreaView>
        );
    }

    const activeOrders = orders.filter(
        (o) => o.status !== 'canceled' && o.status !== 'completed'
    );
    const pastOrders = orders.filter(
        (o) => o.status === 'canceled' || o.status === 'completed'
    );

    return (
        <SafeAreaView style={styles.safeArea}>
            <ScreenHeader title="Your Orders" subtitle={`Logged in as ${role}`} />
            <FlatList
                ListHeaderComponent={
                    <View style={{ padding: 16 }}>
                        <Title style={{ fontWeight: 'bold', fontSize: 22 }}>
                            Welcome, {role} 👋
                        </Title>
                        <Text style={{ marginTop: 4, color: '#666' }}>
                            Here’s your latest orders
                        </Text>
                    </View>
                }
                contentContainerStyle={{ paddingHorizontal: 16, paddingBottom: 32 }}
                data={activeOrders}
                keyExtractor={(item) => item.id}
                ListEmptyComponent={<Text>No active orders.</Text>}
                renderItem={({ item }) => (
                    <OrderCard item={item} role={role} router={router} />
                )}
            />

            {pastOrders.length > 0 && (
                <>
                    <TouchableRipple onPress={() => setShowPastOrders(prev => !prev)}>
                        <View style={styles.sectionHeader}>
                            <Text style={styles.sectionTitle}>Past Orders</Text>
                            <MaterialCommunityIcons
                                name={showPastOrders ? 'chevron-up' : 'chevron-down'}
                                size={24}
                                color="#555"
                            />
                        </View>
                    </TouchableRipple>

                    {showPastOrders && (
                        <FlatList
                            contentContainerStyle={{ paddingHorizontal: 16, paddingBottom: 32 }}
                            data={pastOrders}
                            keyExtractor={(item) => item.id}
                            renderItem={({ item }) => (
                                <OrderCard item={item} role={role} router={router} />
                            )}
                        />
                    )}
                </>
            )}
        </SafeAreaView>
    );
}

function OrderCard({ item, role, router }: { item: Order; role: string; router: any }) {
    return (
        <Surface style={styles.card}>
            <View style={styles.headerRow}>
                <MaterialCommunityIcons
                    name="package-variant-closed"
                    size={20}
                    color="#6200ee"
                />
                <Text style={styles.orderTitle}>Order #{item.id}</Text>
                <Chip style={getStatusChipStyle(item.status)} textStyle={{ color: '#fff' }}>
                    {item.status.toUpperCase()}
                </Chip>
            </View>

            <Divider style={{ marginVertical: 10 }} />

            <View>
                <Text style={styles.itemText}>
                    Location: {item.easyboxAddress}
                </Text>
                <Text style={styles.itemText}>
                    🗓️ Delivery: {new Date(item.deliveryTime).toLocaleString()}
                </Text>
            </View>

            <View style={styles.actions}>
                <Button
                    mode="outlined"
                    onPress={() =>
                        router.push({ pathname: '/order-details', params: { id: item.id } })
                    }
                >
                    View Details
                </Button>
                {(role === 'bakery' && item.status === 'place order') ||
                (role === 'client' && item.status === 'pickup order') ? (
                    <Button
                        mode="contained"
                        onPress={() =>
                            router.push({ pathname: '/report-problem', params: { orderId: item.id } })
                        }
                    >
                        Report
                    </Button>
                ) : null}
            </View>
        </Surface>
    );
}

const styles = StyleSheet.create({
    safeArea: {
        flex: 1,
        backgroundColor: '#f2f2f2',
    },
    card: {
        borderRadius: 16,
        marginBottom: 20,
        padding: 16,
        elevation: 4,
        backgroundColor: '#fff',
    },
    centered: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#fff',
    },
    orderTitle: {
        fontWeight: '600',
        flex: 1,
        fontSize: 16,
        marginLeft: 8,
    },
    itemText: {
        fontSize: 14,
        marginVertical: 2,
    },
    headerRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 8,
    },
    actions: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginTop: 12,
    },
    sectionHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingHorizontal: 16,
        paddingVertical: 12,
        backgroundColor: '#f0f0f0',
        borderTopWidth: 1,
        borderBottomWidth: 1,
        borderColor: '#ddd',
    },
    sectionTitle: {
        fontSize: 16,
        fontWeight: '600',
        color: '#333',
    },
});
