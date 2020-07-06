// Code generated by protoc-gen-go. DO NOT EDIT.
// versions:
// 	protoc-gen-go v1.25.0-devel
// 	protoc        v3.12.0
// source: app.proto

package proto

import (
	proto "github.com/golang/protobuf/proto"
	protoreflect "google.golang.org/protobuf/reflect/protoreflect"
	protoimpl "google.golang.org/protobuf/runtime/protoimpl"
	reflect "reflect"
	sync "sync"
)

const (
	// Verify that this generated code is sufficiently up-to-date.
	_ = protoimpl.EnforceVersion(20 - protoimpl.MinVersion)
	// Verify that runtime/protoimpl is sufficiently up-to-date.
	_ = protoimpl.EnforceVersion(protoimpl.MaxVersion - 20)
)

// This is a compile-time assertion that a sufficiently up-to-date version
// of the legacy proto package is being used.
const _ = proto.ProtoPackageIsVersion4

type Job_JobType int32

const (
	Job_SCRAPING Job_JobType = 0
	Job_CRAWLING Job_JobType = 1
)

// Enum value maps for Job_JobType.
var (
	Job_JobType_name = map[int32]string{
		0: "SCRAPING",
		1: "CRAWLING",
	}
	Job_JobType_value = map[string]int32{
		"SCRAPING": 0,
		"CRAWLING": 1,
	}
)

func (x Job_JobType) Enum() *Job_JobType {
	p := new(Job_JobType)
	*p = x
	return p
}

func (x Job_JobType) String() string {
	return protoimpl.X.EnumStringOf(x.Descriptor(), protoreflect.EnumNumber(x))
}

func (Job_JobType) Descriptor() protoreflect.EnumDescriptor {
	return file_app_proto_enumTypes[0].Descriptor()
}

func (Job_JobType) Type() protoreflect.EnumType {
	return &file_app_proto_enumTypes[0]
}

func (x Job_JobType) Number() protoreflect.EnumNumber {
	return protoreflect.EnumNumber(x)
}

// Deprecated: Use Job_JobType.Descriptor instead.
func (Job_JobType) EnumDescriptor() ([]byte, []int) {
	return file_app_proto_rawDescGZIP(), []int{1, 0}
}

type JobRequest struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Id int32 `protobuf:"varint,1,opt,name=id,proto3" json:"id,omitempty"`
}

func (x *JobRequest) Reset() {
	*x = JobRequest{}
	if protoimpl.UnsafeEnabled {
		mi := &file_app_proto_msgTypes[0]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *JobRequest) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*JobRequest) ProtoMessage() {}

func (x *JobRequest) ProtoReflect() protoreflect.Message {
	mi := &file_app_proto_msgTypes[0]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use JobRequest.ProtoReflect.Descriptor instead.
func (*JobRequest) Descriptor() ([]byte, []int) {
	return file_app_proto_rawDescGZIP(), []int{0}
}

func (x *JobRequest) GetId() int32 {
	if x != nil {
		return x.Id
	}
	return 0
}

type Job struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Type Job_JobType `protobuf:"varint,3,opt,name=type,proto3,enum=Job_JobType" json:"type,omitempty"`
	Id   int32       `protobuf:"varint,1,opt,name=id,proto3" json:"id,omitempty"`
	Urls []string    `protobuf:"bytes,2,rep,name=urls,proto3" json:"urls,omitempty"`
}

func (x *Job) Reset() {
	*x = Job{}
	if protoimpl.UnsafeEnabled {
		mi := &file_app_proto_msgTypes[1]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *Job) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*Job) ProtoMessage() {}

func (x *Job) ProtoReflect() protoreflect.Message {
	mi := &file_app_proto_msgTypes[1]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use Job.ProtoReflect.Descriptor instead.
func (*Job) Descriptor() ([]byte, []int) {
	return file_app_proto_rawDescGZIP(), []int{1}
}

func (x *Job) GetType() Job_JobType {
	if x != nil {
		return x.Type
	}
	return Job_SCRAPING
}

func (x *Job) GetId() int32 {
	if x != nil {
		return x.Id
	}
	return 0
}

func (x *Job) GetUrls() []string {
	if x != nil {
		return x.Urls
	}
	return nil
}

type JobResult struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Job     *Job     `protobuf:"bytes,1,opt,name=job,proto3" json:"job,omitempty"`
	Results []string `protobuf:"bytes,2,rep,name=results,proto3" json:"results,omitempty"`
}

func (x *JobResult) Reset() {
	*x = JobResult{}
	if protoimpl.UnsafeEnabled {
		mi := &file_app_proto_msgTypes[2]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *JobResult) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*JobResult) ProtoMessage() {}

func (x *JobResult) ProtoReflect() protoreflect.Message {
	mi := &file_app_proto_msgTypes[2]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use JobResult.ProtoReflect.Descriptor instead.
func (*JobResult) Descriptor() ([]byte, []int) {
	return file_app_proto_rawDescGZIP(), []int{2}
}

func (x *JobResult) GetJob() *Job {
	if x != nil {
		return x.Job
	}
	return nil
}

func (x *JobResult) GetResults() []string {
	if x != nil {
		return x.Results
	}
	return nil
}

type JobCompletion struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Job *Job `protobuf:"bytes,1,opt,name=job,proto3" json:"job,omitempty"`
}

func (x *JobCompletion) Reset() {
	*x = JobCompletion{}
	if protoimpl.UnsafeEnabled {
		mi := &file_app_proto_msgTypes[3]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *JobCompletion) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*JobCompletion) ProtoMessage() {}

func (x *JobCompletion) ProtoReflect() protoreflect.Message {
	mi := &file_app_proto_msgTypes[3]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use JobCompletion.ProtoReflect.Descriptor instead.
func (*JobCompletion) Descriptor() ([]byte, []int) {
	return file_app_proto_rawDescGZIP(), []int{3}
}

func (x *JobCompletion) GetJob() *Job {
	if x != nil {
		return x.Job
	}
	return nil
}

var File_app_proto protoreflect.FileDescriptor

var file_app_proto_rawDesc = []byte{
	0x0a, 0x09, 0x61, 0x70, 0x70, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x22, 0x1c, 0x0a, 0x0a, 0x4a,
	0x6f, 0x62, 0x52, 0x65, 0x71, 0x75, 0x65, 0x73, 0x74, 0x12, 0x0e, 0x0a, 0x02, 0x69, 0x64, 0x18,
	0x01, 0x20, 0x01, 0x28, 0x05, 0x52, 0x02, 0x69, 0x64, 0x22, 0x72, 0x0a, 0x03, 0x4a, 0x6f, 0x62,
	0x12, 0x20, 0x0a, 0x04, 0x74, 0x79, 0x70, 0x65, 0x18, 0x03, 0x20, 0x01, 0x28, 0x0e, 0x32, 0x0c,
	0x2e, 0x4a, 0x6f, 0x62, 0x2e, 0x4a, 0x6f, 0x62, 0x54, 0x79, 0x70, 0x65, 0x52, 0x04, 0x74, 0x79,
	0x70, 0x65, 0x12, 0x0e, 0x0a, 0x02, 0x69, 0x64, 0x18, 0x01, 0x20, 0x01, 0x28, 0x05, 0x52, 0x02,
	0x69, 0x64, 0x12, 0x12, 0x0a, 0x04, 0x75, 0x72, 0x6c, 0x73, 0x18, 0x02, 0x20, 0x03, 0x28, 0x09,
	0x52, 0x04, 0x75, 0x72, 0x6c, 0x73, 0x22, 0x25, 0x0a, 0x07, 0x4a, 0x6f, 0x62, 0x54, 0x79, 0x70,
	0x65, 0x12, 0x0c, 0x0a, 0x08, 0x53, 0x43, 0x52, 0x41, 0x50, 0x49, 0x4e, 0x47, 0x10, 0x00, 0x12,
	0x0c, 0x0a, 0x08, 0x43, 0x52, 0x41, 0x57, 0x4c, 0x49, 0x4e, 0x47, 0x10, 0x01, 0x22, 0x3d, 0x0a,
	0x09, 0x4a, 0x6f, 0x62, 0x52, 0x65, 0x73, 0x75, 0x6c, 0x74, 0x12, 0x16, 0x0a, 0x03, 0x6a, 0x6f,
	0x62, 0x18, 0x01, 0x20, 0x01, 0x28, 0x0b, 0x32, 0x04, 0x2e, 0x4a, 0x6f, 0x62, 0x52, 0x03, 0x6a,
	0x6f, 0x62, 0x12, 0x18, 0x0a, 0x07, 0x72, 0x65, 0x73, 0x75, 0x6c, 0x74, 0x73, 0x18, 0x02, 0x20,
	0x03, 0x28, 0x09, 0x52, 0x07, 0x72, 0x65, 0x73, 0x75, 0x6c, 0x74, 0x73, 0x22, 0x27, 0x0a, 0x0d,
	0x4a, 0x6f, 0x62, 0x43, 0x6f, 0x6d, 0x70, 0x6c, 0x65, 0x74, 0x69, 0x6f, 0x6e, 0x12, 0x16, 0x0a,
	0x03, 0x6a, 0x6f, 0x62, 0x18, 0x01, 0x20, 0x01, 0x28, 0x0b, 0x32, 0x04, 0x2e, 0x4a, 0x6f, 0x62,
	0x52, 0x03, 0x6a, 0x6f, 0x62, 0x32, 0x58, 0x0a, 0x06, 0x4d, 0x61, 0x73, 0x74, 0x65, 0x72, 0x12,
	0x21, 0x0a, 0x0a, 0x52, 0x65, 0x71, 0x75, 0x65, 0x73, 0x74, 0x4a, 0x6f, 0x62, 0x12, 0x0b, 0x2e,
	0x4a, 0x6f, 0x62, 0x52, 0x65, 0x71, 0x75, 0x65, 0x73, 0x74, 0x1a, 0x04, 0x2e, 0x4a, 0x6f, 0x62,
	0x22, 0x00, 0x12, 0x2b, 0x0a, 0x0b, 0x43, 0x6f, 0x6d, 0x70, 0x6c, 0x65, 0x74, 0x65, 0x4a, 0x6f,
	0x62, 0x12, 0x0a, 0x2e, 0x4a, 0x6f, 0x62, 0x52, 0x65, 0x73, 0x75, 0x6c, 0x74, 0x1a, 0x0e, 0x2e,
	0x4a, 0x6f, 0x62, 0x43, 0x6f, 0x6d, 0x70, 0x6c, 0x65, 0x74, 0x69, 0x6f, 0x6e, 0x22, 0x00, 0x42,
	0x09, 0x5a, 0x07, 0x2e, 0x3b, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x62, 0x06, 0x70, 0x72, 0x6f, 0x74,
	0x6f, 0x33,
}

var (
	file_app_proto_rawDescOnce sync.Once
	file_app_proto_rawDescData = file_app_proto_rawDesc
)

func file_app_proto_rawDescGZIP() []byte {
	file_app_proto_rawDescOnce.Do(func() {
		file_app_proto_rawDescData = protoimpl.X.CompressGZIP(file_app_proto_rawDescData)
	})
	return file_app_proto_rawDescData
}

var file_app_proto_enumTypes = make([]protoimpl.EnumInfo, 1)
var file_app_proto_msgTypes = make([]protoimpl.MessageInfo, 4)
var file_app_proto_goTypes = []interface{}{
	(Job_JobType)(0),      // 0: Job.JobType
	(*JobRequest)(nil),    // 1: JobRequest
	(*Job)(nil),           // 2: Job
	(*JobResult)(nil),     // 3: JobResult
	(*JobCompletion)(nil), // 4: JobCompletion
}
var file_app_proto_depIdxs = []int32{
	0, // 0: Job.type:type_name -> Job.JobType
	2, // 1: JobResult.job:type_name -> Job
	2, // 2: JobCompletion.job:type_name -> Job
	1, // 3: Master.RequestJob:input_type -> JobRequest
	3, // 4: Master.CompleteJob:input_type -> JobResult
	2, // 5: Master.RequestJob:output_type -> Job
	4, // 6: Master.CompleteJob:output_type -> JobCompletion
	5, // [5:7] is the sub-list for method output_type
	3, // [3:5] is the sub-list for method input_type
	3, // [3:3] is the sub-list for extension type_name
	3, // [3:3] is the sub-list for extension extendee
	0, // [0:3] is the sub-list for field type_name
}

func init() { file_app_proto_init() }
func file_app_proto_init() {
	if File_app_proto != nil {
		return
	}
	if !protoimpl.UnsafeEnabled {
		file_app_proto_msgTypes[0].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*JobRequest); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
		file_app_proto_msgTypes[1].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*Job); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
		file_app_proto_msgTypes[2].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*JobResult); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
		file_app_proto_msgTypes[3].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*JobCompletion); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
	}
	type x struct{}
	out := protoimpl.TypeBuilder{
		File: protoimpl.DescBuilder{
			GoPackagePath: reflect.TypeOf(x{}).PkgPath(),
			RawDescriptor: file_app_proto_rawDesc,
			NumEnums:      1,
			NumMessages:   4,
			NumExtensions: 0,
			NumServices:   1,
		},
		GoTypes:           file_app_proto_goTypes,
		DependencyIndexes: file_app_proto_depIdxs,
		EnumInfos:         file_app_proto_enumTypes,
		MessageInfos:      file_app_proto_msgTypes,
	}.Build()
	File_app_proto = out.File
	file_app_proto_rawDesc = nil
	file_app_proto_goTypes = nil
	file_app_proto_depIdxs = nil
}