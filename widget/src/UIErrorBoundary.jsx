import React from 'react';

export default class UiErrorBoundary extends React.Component {
    state = { hasError: false };
    static getDerivedStateFromError() { return { hasError: true }; }
    render() {
        if (this.state.hasError) {
            return (
                <div style={{ padding: 16 }}>
                    Something went wrong – please reload the page.
                </div>
            );
        }
        return this.props.children;
    }
}
