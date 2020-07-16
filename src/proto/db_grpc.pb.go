// Code generated by protoc-gen-go-grpc. DO NOT EDIT.

package proto

import (
	context "context"
	grpc "google.golang.org/grpc"
	codes "google.golang.org/grpc/codes"
	status "google.golang.org/grpc/status"
)

// This is a compile-time assertion to ensure that this generated file
// is compatible with the grpc package it is being compiled against.
const _ = grpc.SupportPackageIsVersion6

// DatabaseClient is the client API for Database service.
//
// For semantics around ctx use and closing/ending streaming RPCs, please refer to https://pkg.go.dev/google.golang.org/grpc/?tab=doc#ClientConn.NewStream.
type DatabaseClient interface {
	Store(ctx context.Context, in *JsonObjects, opts ...grpc.CallOption) (*StorageConfirmation, error)
}

type databaseClient struct {
	cc grpc.ClientConnInterface
}

func NewDatabaseClient(cc grpc.ClientConnInterface) DatabaseClient {
	return &databaseClient{cc}
}

func (c *databaseClient) Store(ctx context.Context, in *JsonObjects, opts ...grpc.CallOption) (*StorageConfirmation, error) {
	out := new(StorageConfirmation)
	err := c.cc.Invoke(ctx, "/Database/Store", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

// DatabaseServer is the server API for Database service.
// All implementations must embed UnimplementedDatabaseServer
// for forward compatibility
type DatabaseServer interface {
	Store(context.Context, *JsonObjects) (*StorageConfirmation, error)
	mustEmbedUnimplementedDatabaseServer()
}

// UnimplementedDatabaseServer must be embedded to have forward compatible implementations.
type UnimplementedDatabaseServer struct {
}

func (*UnimplementedDatabaseServer) Store(context.Context, *JsonObjects) (*StorageConfirmation, error) {
	return nil, status.Errorf(codes.Unimplemented, "method Store not implemented")
}
func (*UnimplementedDatabaseServer) mustEmbedUnimplementedDatabaseServer() {}

func RegisterDatabaseServer(s *grpc.Server, srv DatabaseServer) {
	s.RegisterService(&_Database_serviceDesc, srv)
}

func _Database_Store_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(JsonObjects)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(DatabaseServer).Store(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/Database/Store",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(DatabaseServer).Store(ctx, req.(*JsonObjects))
	}
	return interceptor(ctx, in, info, handler)
}

var _Database_serviceDesc = grpc.ServiceDesc{
	ServiceName: "Database",
	HandlerType: (*DatabaseServer)(nil),
	Methods: []grpc.MethodDesc{
		{
			MethodName: "Store",
			Handler:    _Database_Store_Handler,
		},
	},
	Streams:  []grpc.StreamDesc{},
	Metadata: "db.proto",
}